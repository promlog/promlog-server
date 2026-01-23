package com.promlog.promlog.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record PromptCreateRequest(

        @NotBlank
        @Size(min = 1, max = 200)
        String title,

        @NotBlank(message = "설명은 필수입니다.")
        @Size(min = 1, message = "설명은 1자 이상이어야 합니다.")
        String description,

        @NotBlank(message = "프롬프트는 필수입니다.")
        @Size(min = 1, message = "프롬프트는 1자 이상이어야 합니다.")
        String prompt,

        // 선택
        @Size(max = 5000, message = "tip은 너무 깁니다.") // 원하면 제한 없애도 됨
        String tip,

        @URL
        @Size(max = 500)
        String sourceUrl,

        boolean isAnonymous
) {}
