package com.promlog.promlog.auth.controller;

import com.promlog.promlog.auth.dto.RefreshRequest;
import com.promlog.promlog.auth.dto.RefreshResponse;
import com.promlog.promlog.auth.service.AuthService;
import com.promlog.promlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refreshAccessToken(request.refreshToken()));
    }
}
