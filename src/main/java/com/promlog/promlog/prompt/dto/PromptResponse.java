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
            int bookmarkCount,
            long viewCount,
            long copyCount,
            boolean isLiked,
            boolean isBookmarked
    ) {}

    // 🏷 카테고리/플랫폼 그룹
    public record Tags(
            List<TagDto> categories,
            List<TagDto> platforms
    ) {}

    // ✅ 기존 호출부 호환용 (기본값: false, false)
    public static PromptResponse from(Prompt p) {
        return from(p, false, false);
    }

    // ✅ 기존 호출부 호환용 (isBookmarked 기본 false)
    public static PromptResponse from(Prompt p, boolean isLiked) {
        return from(p, isLiked, false);
    }

    // ✅ 신규: 좋아요 + 북마크 여부 모두 포함
    public static PromptResponse from(Prompt p, boolean isLiked, boolean isBookmarked) {
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
                        p.getAuthorAccountId(), // 익명이어도 id를 내려줄지 정책에 따라 바꿔도 됨
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
                        p.getBookmarkCount(),
                        p.getViewCount(),
                        p.getCopyCount(),
                        isLiked,
                        isBookmarked
                ),
                new Tags(categories, platforms)
        );
    }
}
