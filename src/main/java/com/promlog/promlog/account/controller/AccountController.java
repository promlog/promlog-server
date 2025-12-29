package com.promlog.promlog.account.controller;

import com.promlog.promlog.account.dto.AccountDeleteResponse;
import com.promlog.promlog.account.dto.NicknameUpdateRequest;
import com.promlog.promlog.account.dto.NicknameUpdateResponse;
import com.promlog.promlog.account.service.AccountService;
import com.promlog.promlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ApiResponse<?> getMe(Authentication authentication) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(accountService.getMyAccount(accountId));
    }

    @PatchMapping("/me")
    public ApiResponse<NicknameUpdateResponse> updateNickname(
            Authentication authentication,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(accountService.updateNickname(accountId, request.nickname()));
    }

    @DeleteMapping("/me")
    public ApiResponse<AccountDeleteResponse> deleteMe(Authentication authentication) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(accountService.deleteMe(accountId));
    }

}
