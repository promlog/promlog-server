package com.promlog.promlog.prompt.controller;

import com.promlog.promlog.global.response.ApiResponse;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptListResponse;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.dto.PromptUpdateRequest;
import com.promlog.promlog.prompt.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * sort:
     * - latest (기본)
     * - likes  (좋아요 많은 순)
     * - views  (조회수 많은 순)
     *
     * 예)
     * /api/prompts?sort=likes&categoryIds=1,3&platformIds=2&page=1&size=20
     */
    @GetMapping
    public ApiResponse<PromptListResponse> list(
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> platformIds
    ) {
        return ApiResponse.ok(promptService.list(sort, page, size, categoryIds, platformIds));
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

    @DeleteMapping("/{promptId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> delete(
            Authentication authentication,
            @PathVariable Long promptId
    ) {
        long accountId = (long) authentication.getPrincipal();
        promptService.delete(accountId, promptId);
        return ApiResponse.ok(java.util.Map.of("deleted", true));
    }

    @PostMapping("/{promptId}/copy")
    public ApiResponse<?> copy(@PathVariable Long promptId) {
        return ApiResponse.ok(promptService.copy(promptId));
    }

    @GetMapping("/me")
    public ApiResponse<PromptListResponse> myPrompts(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.listMine(accountId, page, size));
    }
}
