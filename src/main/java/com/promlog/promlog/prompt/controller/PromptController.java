package com.promlog.promlog.prompt.controller;

import com.promlog.promlog.global.response.ApiResponse;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.dto.PromptUpdateRequest;
import com.promlog.promlog.prompt.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.promlog.promlog.prompt.dto.PromptListResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

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
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.create(accountId, req));
    }

    @GetMapping
    public ApiResponse<PromptListResponse> list(
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ApiResponse.ok(promptService.list(sort, page, size));
    }

    @GetMapping("/{promptId}")
    public ApiResponse<PromptResponse> detail(@PathVariable Long promptId) {
        return ApiResponse.ok(promptService.getDetail(promptId));
    }

    @PatchMapping("/{promptId}")
    public ApiResponse<PromptResponse> update(
            Authentication authentication,
            @PathVariable Long promptId,
            @Valid @RequestBody PromptUpdateRequest req
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.update(accountId, promptId, req));
    }
}
