package com.promlog.promlog.prompt.dto;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> items,
        CursorMeta meta
) {
    public record CursorMeta(
            int size,
            boolean hasNext,
            String nextCursorCreatedAt, // ISO-8601 string (ex: 2026-02-20T12:00:00.123)
            Long nextCursorId
    ) {}
}