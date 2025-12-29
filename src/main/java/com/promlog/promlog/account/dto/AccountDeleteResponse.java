package com.promlog.promlog.account.dto;

import java.time.LocalDateTime;

public record AccountDeleteResponse(
        LocalDateTime deletedAt,
        LocalDateTime rejoinAllowedAt
) {}
