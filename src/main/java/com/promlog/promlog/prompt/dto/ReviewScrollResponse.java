package com.promlog.promlog.prompt.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewScrollResponse(
        List<ReviewResponse> items,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorId
) {}