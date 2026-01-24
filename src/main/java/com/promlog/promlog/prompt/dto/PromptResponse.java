package com.promlog.promlog.prompt.dto;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PromptResponse(
        Long id,
        Long authorAccountId,
        String authorNickname,
        String title,
        String description,
        String prompt,
        String tip,
        String sourceUrl,
        boolean isAnonymous,
        PromptStatus status,
        int likeCount,
        long viewCount,
        long copyCount,
        LocalDateTime createdAt,

        // ✅ 추가
        List<TagDto> categories,
        List<TagDto> platforms
) {
    public static PromptResponse from(Prompt p) {
        String nickname = p.isAnonymous() ? null : p.getAuthor().getNickname();

        List<TagDto> categories = p.getPromptCategories().stream()
                .map(pc -> new TagDto(
                        pc.getCategory().getId(),
                        pc.getCategory().getName(),
                        pc.getCategory().getSlug()
                ))
                .toList();

        List<TagDto> platforms = p.getPromptPlatforms().stream()
                .map(pp -> new TagDto(
                        pp.getPlatform().getId(),
                        pp.getPlatform().getName(),
                        pp.getPlatform().getSlug()
                ))
                .toList();

        return new PromptResponse(
                p.getId(),
                p.getAuthorAccountId(),
                nickname,
                p.getTitle(),
                p.getDescription(),
                p.getPrompt(),
                p.getTip(),
                p.getSourceUrl(),
                p.isAnonymous(),
                p.getStatus(),
                p.getLikeCount(),
                p.getViewCount(),
                p.getCopyCount(),
                p.getCreatedAt(),
                categories,
                platforms
        );
    }
}
