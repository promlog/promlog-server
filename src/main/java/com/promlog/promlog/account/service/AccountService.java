package com.promlog.promlog.account.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.domain.AccountStatus;
import com.promlog.promlog.account.dto.AccountDeleteResponse;
import com.promlog.promlog.account.dto.AccountMeResponse;
import com.promlog.promlog.account.dto.NicknameUpdateResponse;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.oauthidentity.repository.OAuthIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;

    public AccountService(AccountRepository accountRepository,
                          OAuthIdentityRepository oAuthIdentityRepository) {
        this.accountRepository = accountRepository;
        this.oAuthIdentityRepository = oAuthIdentityRepository;
    }

    public AccountMeResponse getMyAccount(long accountId) {
        Account account = getAccountOrThrow(accountId);

        // ✅ 정책: 내 정보 조회도 상태 체크를 동일하게 적용
        // (원하면 DELETED만 막고 SUSPENDED는 허용 같은 정책도 가능)
        validateAccountActiveOrThrow(account);

        return AccountMeResponse.from(account);
    }

    @Transactional
    public NicknameUpdateResponse updateNickname(long accountId, String nickname) {
        Account account = getAccountOrThrow(accountId);

        validateAccountActiveOrThrow(account);

        // 최소 방어(원하면 Controller @Valid로만 처리하고 이건 제거 가능)
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "nickname이 필요합니다."
            );
        }

        account.changeNickname(nickname);
        return new NicknameUpdateResponse(account.getId(), account.getNickname());
    }

    @Transactional
    public AccountDeleteResponse deleteMe(long accountId) {
        Account account = getAccountOrThrow(accountId);

        // 이미 탈퇴한 계정이면 "idempotent"하게 같은 형태로 응답
        if (account.getStatus() == AccountStatus.DELETED) {
            LocalDateTime deletedAt = (account.getDeletedAt() != null)
                    ? account.getDeletedAt()
                    : LocalDateTime.now();

            return new AccountDeleteResponse(deletedAt, deletedAt.plusDays(7));
        }

        LocalDateTime now = LocalDateTime.now();

        // ✅ 소프트 삭제
        account.softDelete(now);

        // ✅ oauth_identities도 같이 소프트 삭제 (연동 끊기 + 재가입 제한 트리거용)
        oAuthIdentityRepository.softDeleteAllByAccountId(accountId, now);

        return new AccountDeleteResponse(now, now.plusDays(7));
    }

    // -----------------------------
    // private helpers
    // -----------------------------

    private Account getAccountOrThrow(long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "계정을 찾을 수 없습니다."
                ));
    }

    /**
     * ✅ 공통 상태 검증: DELETED / SUSPENDED는 서비스 액션 차단
     * - DELETED: ACCOUNT_DELETED
     * - SUSPENDED: ACCOUNT_SUSPENDED (until이 null이면 무기한 정지로 간주)
     */
    private void validateAccountActiveOrThrow(Account account) {
        LocalDateTime now = LocalDateTime.now();

        if (account.getStatus() == AccountStatus.DELETED) {
            LocalDateTime deletedAt = account.getDeletedAt();
            LocalDateTime rejoinAllowedAt = (deletedAt == null) ? null : deletedAt.plusDays(7);

            // 메시지는 너가 OAuthService에서 쓰는 톤이랑 맞춤
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DELETED,
                    "탈퇴 처리된 계정입니다. 재가입을 진행해 주세요.",
                    (rejoinAllowedAt == null) ? null : Map.of("rejoinAllowedAt", rejoinAllowedAt)
            );
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            LocalDateTime until = account.getSuspendedUntil();

            if (until == null || now.isBefore(until)) {
                Object details = (until == null)
                        ? Map.of("suspendedUntil", "INDEFINITE")
                        : Map.of("suspendedUntil", until);

                throw new BusinessException(
                        ErrorCode.ACCOUNT_SUSPENDED,
                        "정지된 계정입니다.",
                        details
                );
            }
        }
    }
}