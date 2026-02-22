package com.promlog.promlog.auth.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.domain.AccountStatus;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.auth.dto.RefreshResponse;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.security.jwt.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccountRepository accountRepository;

    public AuthService(JwtTokenProvider jwtTokenProvider, AccountRepository accountRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accountRepository = accountRepository;
    }

    public RefreshResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "refreshToken이 필요합니다."
            );
        }

        // 1) refresh token 검증 + accountId(subject) 추출
        final long accountId;
        try {
            accountId = jwtTokenProvider.parseRefreshSubjectAsLong(refreshToken);
        } catch (BusinessException e) {
            // JwtTokenProvider 내부에서 BusinessException을 던지는 경우가 있다면 그대로 전파
            throw e;
        } catch (RuntimeException e) {
            // 라이브러리 예외/파싱 예외 등 -> 401로 정규화
            log.warn("[RefreshTokenInvalid] msg={}", String.valueOf(rootCause(e).getMessage()));
            throw new BusinessException(
                    ErrorCode.TOKEN_INVALID,
                    "유효하지 않은 refresh token 입니다.",
                    Map.of("reason", "REFRESH_TOKEN_INVALID")
            );
        }

        // 2) 계정 조회
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "계정을 찾을 수 없습니다."
                ));

        // 3) 계정 상태 체크 (토큰 발급 전에 차단)
        validateAccountStatus(account);

        // 4) 새 access token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole().name());

        return new RefreshResponse(newAccessToken);
    }

    private void validateAccountStatus(Account account) {
        LocalDateTime now = LocalDateTime.now();

        if (account.getStatus() == AccountStatus.DELETED) {
            // 네 ErrorCode에 ACCOUNT_DELETED가 있으니 그걸 쓰는 게 일관성 좋음
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DELETED,
                    "탈퇴 처리된 계정입니다. 재가입을 진행해 주세요."
            );
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            LocalDateTime until = account.getSuspendedUntil();

            // until == null이면 "무기한 정지"로 보는 게 일반적 -> 차단
            if (until == null || now.isBefore(until)) {
                throw new BusinessException(
                        ErrorCode.ACCOUNT_SUSPENDED,
                        "정지된 계정입니다.",
                        (until == null) ? Map.of("suspendedUntil", "INDEFINITE")
                                : Map.of("suspendedUntil", until)
                );
            }
        }
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        int guard = 0;
        while (cur.getCause() != null && cur.getCause() != cur && guard++ < 20) {
            cur = cur.getCause();
        }
        return cur;
    }
}