package com.promlog.promlog.prompt.category.dto;

import com.promlog.promlog.prompt.category.domain.Category;

public record CategoryResponse(
        Long id,
        String name,
        String slug
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
    }
}
