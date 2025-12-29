package com.promlog.promlog.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @NotBlank(message = "nickname은 필수입니다.")
        @Size(min = 1, max = 50, message = "nickname은 1~50자여야 합니다.")
        String nickname
) {}
