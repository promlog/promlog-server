package com.promlog.promlog.global.response;

public record PageMeta(
        int page,       // 1-base
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}
