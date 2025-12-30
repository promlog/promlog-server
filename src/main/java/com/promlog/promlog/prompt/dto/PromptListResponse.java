package com.promlog.promlog.prompt.dto;

import com.promlog.promlog.global.response.PageMeta;

import java.util.List;

public record PromptListResponse(
        List<PromptResponse> items,
        PageMeta meta
) {}
