package com.promlog.promlog.auth.controller;

import com.promlog.promlog.auth.infra.kakao.KakaoOAuthProperties;
import com.promlog.promlog.auth.service.OAuthService;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthService oauthService;
    private final KakaoOAuthProperties kakaoProps;

    public OAuthController(OAuthService oauthService, KakaoOAuthProperties kakaoProps) {
        this.oauthService = oauthService;
        this.kakaoProps = kakaoProps;
    }

    @GetMapping("/kakao/authorize")
    public void kakaoAuthorize(HttpServletResponse response) throws IOException {
        String authorizeUrl =
                "https://kauth.kakao.com/oauth/authorize" +
                        "?client_id=" + url(kakaoProps.clientId()) +
                        "&redirect_uri=" + url(kakaoProps.redirectUri()) +
                        "&response_type=code";

        response.sendRedirect(authorizeUrl);
    }

    @GetMapping("/kakao/callback")
    public ApiResponse<?> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) {
        // 1) code가 없으면 -> 카카오가 실패/취소로 보낸 것
        if (code == null || code.isBlank()) {
            // 여기서 원인을 그대로 내려주면 다음 디버깅이 쉬움
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "카카오 인증 실패 또는 취소",
                    java.util.Map.of(
                            "error", error,
                            "errorDescription", errorDescription
                    )
            );
        }

        // 2) 정상 성공
        return ApiResponse.ok(oauthService.kakaoLogin(code));
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
