package com.promlog.promlog.prompt.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record PromptUpdateRequest(

        @Size(min = 1, max = 200, message = "title은 1~200자여야 합니다.")
        String title,

        @Size(min = 1, message = "body는 1자 이상이어야 합니다.")
        String body,

        @URL(message = "sourceUrl은 URL 형식이어야 합니다.")
        @Size(max = 500, message = "sourceUrl은 최대 500자입니다.")
        String sourceUrl,

        Boolean isAnonymous
) {}
