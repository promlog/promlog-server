package com.promlog.promlog.auth.dto;

import com.promlog.promlog.account.domain.AccountStatus;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        AccountDto account
) {
    public record AccountDto(
            long id,
            String nickname,
            String role,
            AccountStatus status
    ) {}
}