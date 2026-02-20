package com.promlog.promlog.prompt.dto;

import com.promlog.promlog.prompt.domain.PromptReview;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long promptId,
        Long accountId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(PromptReview r) {
        return new ReviewResponse(
                r.getId(),
                r.getPromptId(),
                r.getAccountId(),
                r.getContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
