package com.promlog.promlog.auth.controller;

import com.promlog.promlog.auth.dto.RefreshResponse;
import com.promlog.promlog.auth.service.AuthService;
import com.promlog.promlog.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        return ApiResponse.ok(authService.refreshAccessToken(refreshToken));
    }

    /**
     * ✅ 로그아웃: refresh_token 쿠키 삭제만 수행
     * (로그인 때와 동일 옵션으로 삭제해야 정상 제거됨)
     */
    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletResponse response) {

        ResponseCookie deleteRefreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)              // 로그인 때와 동일 (로컬 기준)
                .path("/api/auth")          // 로그인 때와 동일
                .sameSite("Lax")            // 로그인 때와 동일
                .maxAge(0)                  // 즉시 만료
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

        return ApiResponse.ok(Map.of("loggedOut", true));
    }
}
