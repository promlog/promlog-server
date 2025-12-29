package com.promlog.promlog.auth.infra.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount,
        Properties properties
) {
    public record KakaoAccount(
            Boolean hasEmail,
            String email
    ) {}

    public record Properties(
            String nickname,
            @JsonProperty("profile_image") String profileImage,
            @JsonProperty("thumbnail_image") String thumbnailImage
    ) {}
}