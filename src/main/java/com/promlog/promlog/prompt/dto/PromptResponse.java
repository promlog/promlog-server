package com.promlog.promlog.prompt.dto;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PromptResponse(
        Long id,
        PromptStatus status,

        Author author,
        Content content,
        Stats stats,
        Tags tags
) {

    // 👤 작성자 정보 그룹
    public record Author(
            Long id,
            String nickname,
            boolean isAnonymous
    ) {}

    // 📝 본문 정보 그룹
    public record Content(
            String title,
            String description,
            String prompt,
            String tip,
            String sourceUrl,
            LocalDateTime createdAt
    ) {}

    // 📊 통계 정보 그룹
    public record Stats(
            int likeCount,
            long viewCount,
            long copyCount
    ) {}

    // 🏷 카테고리/플랫폼 그룹
    public record Tags(
            List<TagDto> categories,
            List<TagDto> platforms
    ) {}

    public static PromptResponse from(Prompt p) {
        boolean anonymous = p.isAnonymous();
        String nickname = anonymous ? null : p.getAuthor().getNickname();

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
                p.getStatus(),
                new Author(
                        p.getAuthorAccountId(),
                        nickname,
                        anonymous
                ),
                new Content(
                        p.getTitle(),
                        p.getDescription(),
                        p.getPrompt(),
                        p.getTip(),
                        p.getSourceUrl(),
                        p.getCreatedAt()
                ),
                new Stats(
                        p.getLikeCount(),
                        p.getViewCount(),
                        p.getCopyCount()
                ),
                new Tags(categories, platforms)
        );
    }
}
