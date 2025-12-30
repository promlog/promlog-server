package com.promlog.promlog.prompt.controller;

import com.promlog.promlog.global.response.ApiResponse;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PromptResponse> create(
            Authentication authentication,
            @Valid @RequestBody PromptCreateRequest req
    ) {
        long accountId = (long) authentication.getPrincipal(); // ✅ AccountController와 동일
        return ApiResponse.ok(promptService.create(accountId, req));
    }
}
