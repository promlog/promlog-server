package com.promlog.promlog.auth.service;

import com.promlog.promlog.auth.dto.RefreshResponse;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.security.jwt.JwtTokenProvider;
import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.domain.AccountStatus;
import com.promlog.promlog.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "refreshToken이 필요합니다.");
        }

        // 1) refresh token 검증 + accountId 추출
        long accountId = jwtTokenProvider.parseRefreshSubjectAsLong(refreshToken);

        // 2) 계정 상태 체크
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다."));

        // 상태에 따라 막기
        if (account.getStatus() == AccountStatus.DELETED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "탈퇴한 계정입니다.");
        }
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            LocalDateTime until = account.getSuspendedUntil();
            if (until != null && LocalDateTime.now().isBefore(until)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "정지된 계정입니다.");
            }
        }

        // 3) 새 access token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole().name());

        return new RefreshResponse(newAccessToken);
    }
}
