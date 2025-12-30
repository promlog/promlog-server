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

    // ✅ 프론트가 code 받는 경우: 이 콜백은 이제 "프론트"로 감
    // 그래서 이 엔드포인트는 거의 안 쓰게 됨(남겨도 되는데 실제로는 호출 안 됨)
    @GetMapping("/kakao/callback")
    public ApiResponse<?> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "카카오 인증 실패 또는 취소",
                    java.util.Map.of(
                            "error", error,
                            "errorDescription", errorDescription
                    )
            );
        }
        return ApiResponse.ok(oauthService.kakaoLogin(code));
    }

    // ✅ NEW: 프론트가 받은 code로 로그인 처리하는 API
    @PostMapping("/kakao/code")
    public ApiResponse<?> kakaoCodeLogin(@RequestBody KakaoCodeRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "code가 비어있습니다.", null);
        }
        return ApiResponse.ok(oauthService.kakaoLogin(request.code()));
    }

    public record KakaoCodeRequest(String code) {}

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
