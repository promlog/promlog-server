package com.promlog.promlog.prompt.platform.dto;

import com.promlog.promlog.prompt.platform.domain.Platform;

public record PlatformResponse(
        Long id,
        String name,
        String slug
) {
    public static PlatformResponse from(Platform p) {
        return new PlatformResponse(p.getId(), p.getName(), p.getSlug());
    }
}
