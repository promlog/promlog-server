package com.promlog.promlog.account.service;

import com.promlog.promlog.account.dto.AccountDeleteResponse;
import com.promlog.promlog.account.dto.AccountMeResponse;
import com.promlog.promlog.account.dto.NicknameUpdateResponse;
import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.domain.AccountStatus;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.oauthidentity.repository.OAuthIdentityRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다."));

        return AccountMeResponse.from(account);
    }

    @Transactional
    public NicknameUpdateResponse updateNickname(long accountId, String nickname) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다."));

        // (권장) 상태 체크
        if (account.getStatus() == AccountStatus.DELETED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "탈퇴한 계정입니다.");
        }
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            LocalDateTime until = account.getSuspendedUntil();
            if (until != null && LocalDateTime.now().isBefore(until)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "정지된 계정입니다.");
            }
        }

        account.changeNickname(nickname); // 엔티티 메서드로 변경 추천
        return new NicknameUpdateResponse(account.getId(), account.getNickname());
    }

    @Transactional
    public AccountDeleteResponse deleteMe(long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다."));

        // 이미 탈퇴한 계정이면 (정책 선택)
        if (account.getStatus() == AccountStatus.DELETED) {
            // 이미 deleted_at이 있으면 그걸 기준으로 rejoinAllowedAt 계산해서 그대로 응답
            LocalDateTime deletedAt = account.getDeletedAt() != null ? account.getDeletedAt() : LocalDateTime.now();
            return new AccountDeleteResponse(deletedAt, deletedAt.plusDays(7));
        }

        LocalDateTime now = LocalDateTime.now();

        // 1) accounts 소프트 삭제
        account.softDelete(now);

        // 2) oauth_identities도 같이 소프트 삭제 (연동 끊기 + 7일 재가입 트리거용 기록)
        oAuthIdentityRepository.softDeleteAllByAccountId(accountId, now);

        return new AccountDeleteResponse(now, now.plusDays(7));
    }
}
