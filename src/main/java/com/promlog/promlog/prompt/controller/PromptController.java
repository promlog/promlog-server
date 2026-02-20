package com.promlog.promlog.prompt.controller;

import com.promlog.promlog.global.response.ApiResponse;
import com.promlog.promlog.prompt.dto.BookmarkResponse;
import com.promlog.promlog.prompt.dto.LikeResponse;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptListResponse;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.dto.PromptUpdateRequest;
import com.promlog.promlog.prompt.dto.ReviewCreateRequest;
import com.promlog.promlog.prompt.dto.ReviewResponse;
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

    @GetMapping
    public ApiResponse<PromptListResponse> list(
            Authentication authentication, // ✅ 로그인 안 하면 null
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> platformIds
    ) {
        Long viewerAccountId = (authentication == null) ? null : (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.list(viewerAccountId, sort, page, size, categoryIds, platformIds));
    }

    @GetMapping("/{promptId}")
    public ApiResponse<PromptResponse> detail(
            Authentication authentication, // ✅ 로그인 안 하면 null
            @PathVariable Long promptId
    ) {
        Long viewerAccountId = (authentication == null) ? null : (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.getDetail(viewerAccountId, promptId));
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

    /* ===============================
       likes
       =============================== */

    @PostMapping("/{promptId}/likes")
    public ApiResponse<LikeResponse> like(
            Authentication authentication,
            @PathVariable Long promptId
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.like(accountId, promptId));
    }

    @DeleteMapping("/{promptId}/likes")
    public ApiResponse<LikeResponse> unlike(
            Authentication authentication,
            @PathVariable Long promptId
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.unlike(accountId, promptId));
    }

    @GetMapping("/me/likes")
    public ApiResponse<PromptListResponse> myLikedPrompts(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.listLiked(accountId, page, size));
    }

    /* ===============================
       bookmarks
       =============================== */

    @PostMapping("/{promptId}/bookmarks")
    public ApiResponse<BookmarkResponse> bookmark(
            Authentication authentication,
            @PathVariable Long promptId
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.bookmark(accountId, promptId));
    }

    @DeleteMapping("/{promptId}/bookmarks")
    public ApiResponse<BookmarkResponse> unbookmark(
            Authentication authentication,
            @PathVariable Long promptId
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.unbookmark(accountId, promptId));
    }

    @GetMapping("/me/bookmarks")
    public ApiResponse<PromptListResponse> myBookmarkedPrompts(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.listBookmarked(accountId, page, size));
    }

    /* ===============================
       reviews
       =============================== */

    // ✅ 리뷰 작성 (로그인 필수)
    @PostMapping("/{promptId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            Authentication authentication,
            @PathVariable Long promptId,
            @Valid @RequestBody ReviewCreateRequest req
    ) {
        long accountId = (long) authentication.getPrincipal();
        return ApiResponse.ok(promptService.createReview(accountId, promptId, req));
    }
}
