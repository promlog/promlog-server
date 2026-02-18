package com.promlog.promlog.prompt.dto;

public record BookmarkResponse(
        boolean bookmarked,
        int bookmarkCount
) {
}
