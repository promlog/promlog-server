package com.promlog.promlog.prompt.dto;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;

import java.time.LocalDateTime;

public record PromptResponse(
        Long id,
        Long authorAccountId,
        String title,
        String body,
        String sourceUrl,
        boolean isAnonymous,
        PromptStatus status,
        int likeCount,
        long viewCount,
        long copyCount,
        LocalDateTime createdAt
) {
    public static PromptResponse from(Prompt p) {
        return new PromptResponse(
                p.getId(),
                p.getAuthorAccountId(),
                p.getTitle(),
                p.getBody(),
                p.getSourceUrl(),
                p.isAnonymous(),
                p.getStatus(),
                p.getLikeCount(),
                p.getViewCount(),
                p.getCopyCount(),
                p.getCreatedAt()
        );
    }
}
