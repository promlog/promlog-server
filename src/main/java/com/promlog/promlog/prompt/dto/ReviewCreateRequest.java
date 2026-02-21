package com.promlog.promlog.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotBlank(message = "후기 내용을 입력해 주세요.")
        @Size(max = 2000, message = "후기는 최대 2000자까지 가능합니다.")
        String content
) {}
