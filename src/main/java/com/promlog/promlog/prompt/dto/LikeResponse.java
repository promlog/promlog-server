package com.promlog.promlog.prompt.dto;

public record LikeResponse(
        boolean liked,
        int likeCount
) {}
