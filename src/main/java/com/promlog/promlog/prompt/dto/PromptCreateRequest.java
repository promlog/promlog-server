package com.promlog.promlog.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record PromptCreateRequest(

        @NotBlank
        @Size(min = 1, max = 200)
        String title,

        @NotBlank
        @Size(min = 1)
        String body,

        @URL
        @Size(max = 500)
        String sourceUrl,

        boolean isAnonymous
) {}
