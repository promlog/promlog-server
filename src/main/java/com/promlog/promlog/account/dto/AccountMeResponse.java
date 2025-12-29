package com.promlog.promlog.account.dto;

import com.promlog.promlog.account.domain.Account;

import java.time.LocalDateTime;

public record AccountMeResponse(
        long id,
        String nickname,
        String role,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static AccountMeResponse from(Account account) {
        return new AccountMeResponse(
                account.getId(),
                account.getNickname(),
                account.getRole().name(),
                account.getStatus().name(),
                account.getLastLoginAt(),
                account.getCreatedAt()
        );
    }
}
