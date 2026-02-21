package com.promlog.promlog.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(
        @NotBlank(message = "리뷰 내용은 비어있을 수 없습니다.")
        @Size(max = 2000, message = "리뷰는 최대 2000자까지 가능합니다.")
        String content
) {}