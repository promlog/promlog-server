package com.promlog.promlog.prompt.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.domain.AccountStatus;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.response.PageMeta;
import com.promlog.promlog.prompt.category.repository.CategoryRepository;
import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptReview;
import com.promlog.promlog.prompt.domain.PromptStatus;
import com.promlog.promlog.prompt.dto.*;
import com.promlog.promlog.prompt.platform.repository.PlatformRepository;
import com.promlog.promlog.prompt.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptLikeRepository promptLikeRepository;
    private final PromptBookmarkRepository promptBookmarkRepository;
    private final PromptReviewRepository promptReviewRepository;

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final PlatformRepository platformRepository;

    public PromptService(
            PromptRepository promptRepository,
            PromptLikeRepository promptLikeRepository,
            PromptBookmarkRepository promptBookmarkRepository,
            PromptReviewRepository promptReviewRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            PlatformRepository platformRepository
    ) {
        this.promptRepository = promptRepository;
        this.promptLikeRepository = promptLikeRepository;
        this.promptBookmarkRepository = promptBookmarkRepository;
        this.promptReviewRepository = promptReviewRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.platformRepository = platformRepository;
    }

    /* ===============================
       prompts
       =============================== */

    @Transactional
    public PromptResponse create(long accountId, PromptCreateRequest req) {
        // ✅ 계정 존재 + 상태 체크
        Account author = getAccountActiveOrThrow(accountId, "작성자 계정을 찾을 수 없습니다.");

        // ✅ title NPE 방지 + validation
        String title = (req.title() == null) ? "" : req.title().trim();
        if (title.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "제목은 비어있을 수 없습니다.");
        }

        Prompt prompt = new Prompt(
                author,
                title,
                req.description(),
                req.prompt(),
                req.tip(),
                req.sourceUrl(),
                req.isAnonymous()
        );

        Prompt saved = promptRepository.save(prompt);

        List<Long> categoryIds = safeIds(req.categoryIds());
        List<Long> platformIds = safeIds(req.platformIds());

        if (!categoryIds.isEmpty()) {
            var categories = categoryRepository.findByIdInAndDeletedAtIsNull(categoryIds);
            validateAllIdsExist(categoryIds, categories.size(), "categoryIds");
            saved.replaceCategories(categories);
        } else {
            saved.replaceCategories(List.of());
        }

        if (!platformIds.isEmpty()) {
            var platforms = platformRepository.findByIdInAndDeletedAtIsNull(platformIds);
            validateAllIdsExist(platformIds, platforms.size(), "platformIds");
            saved.replacePlatforms(platforms);
        } else {
            saved.replacePlatforms(List.of());
        }

        var refreshed = promptRepository
                .findDetailWithAuthorAndTags(saved.getId(), PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        return PromptResponse.from(refreshed, false, false);
    }

    @Transactional(readOnly = true)
    public PromptListResponse list(Long viewerAccountId, String sort, int page, int size,
                                   List<Long> categoryIds, List<Long> platformIds) {

        if (page < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");

        String s = (sort == null || sort.isBlank()) ? "latest" : sort.trim().toLowerCase();

        Sort springSort = switch (s) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "likes" -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"));
            case "views" -> Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
            default -> throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "지원하지 않는 sort 입니다. (latest/likes/views)",
                    Map.of("sort", s)
            );
        };

        PageRequest pageable = PageRequest.of(page - 1, size, springSort);

        List<Long> cids = safeIds(categoryIds);
        List<Long> pids = safeIds(platformIds);

        Specification<Prompt> spec = Specification
                .where(PromptSpecifications.baseVisible())
                .and(PromptSpecifications.hasAnyCategoryIds(cids))
                .and(PromptSpecifications.hasAnyPlatformIds(pids));

        var result = promptRepository.findAll(spec, pageable);

        List<Long> promptIds = result.getContent().stream()
                .map(Prompt::getId)
                .filter(Objects::nonNull)
                .toList();

        final Set<Long> likedPromptIds =
                (viewerAccountId == null || promptIds.isEmpty())
                        ? Collections.emptySet()
                        : new HashSet<>(promptLikeRepository.findActiveLikedPromptIds(viewerAccountId, promptIds));

        final Set<Long> bookmarkedPromptIds =
                (viewerAccountId == null || promptIds.isEmpty())
                        ? Collections.emptySet()
                        : new HashSet<>(promptBookmarkRepository.findActiveBookmarkedPromptIdsIn(viewerAccountId, promptIds));

        List<PromptResponse> items = result.getContent()
                .stream()
                .map(p -> PromptResponse.from(
                        p,
                        likedPromptIds.contains(p.getId()),
                        bookmarkedPromptIds.contains(p.getId())
                ))
                .toList();

        PageMeta meta = new PageMeta(
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return new PromptListResponse(items, meta);
    }

    @Transactional
    public PromptResponse getDetail(Long viewerAccountId, Long promptId) {

        int updated = promptRepository.increaseViewCount(promptId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다.");
        }

        var prompt = promptRepository
                .findDetailWithAuthorAndTags(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        boolean isLiked = false;
        boolean isBookmarked = false;

        if (viewerAccountId != null) {
            isLiked = promptLikeRepository.existsActive(promptId, viewerAccountId);
            isBookmarked = promptBookmarkRepository.existsActive(promptId, viewerAccountId);
        }

        return PromptResponse.from(prompt, isLiked, isBookmarked);
    }

    @Transactional
    public PromptResponse update(long accountId, Long promptId, PromptUpdateRequest req) {
        // ✅ 계정 상태 체크 (수정은 "행동"이므로 차단)
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        var prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        if (!prompt.getAuthorAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "작성자만 수정할 수 있습니다.");
        }

        // ✅ title null 방어
        if (req.title() != null && req.title().trim().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "제목은 비어있을 수 없습니다.");
        }

        prompt.update(
                req.title(),
                req.description(),
                req.prompt(),
                req.tip(),
                req.sourceUrl(),
                req.isAnonymous()
        );

        if (req.categoryIds() != null) {
            var categoryIds = safeIds(req.categoryIds());
            if (categoryIds.isEmpty()) {
                prompt.replaceCategories(List.of());
            } else {
                var categories = categoryRepository.findByIdInAndDeletedAtIsNull(categoryIds);
                validateAllIdsExist(categoryIds, categories.size(), "categoryIds");
                prompt.replaceCategories(categories);
            }
        }

        if (req.platformIds() != null) {
            var platformIds = safeIds(req.platformIds());
            if (platformIds.isEmpty()) {
                prompt.replacePlatforms(List.of());
            } else {
                var platforms = platformRepository.findByIdInAndDeletedAtIsNull(platformIds);
                validateAllIdsExist(platformIds, platforms.size(), "platformIds");
                prompt.replacePlatforms(platforms);
            }
        }

        var refreshed = promptRepository
                .findDetailWithAuthorAndTags(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        // viewer 정보가 없으니 false/false 유지
        return PromptResponse.from(refreshed, false, false);
    }

    @Transactional
    public void delete(long accountId, Long promptId) {
        // ✅ 계정 상태 체크
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        var prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        if (!prompt.getAuthorAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "작성자만 삭제할 수 있습니다.");
        }

        prompt.softDelete(LocalDateTime.now());
    }

    @Transactional
    public Map<String, Object> copy(Long promptId) {

        int updated = promptRepository.increaseCopyCount(promptId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다.");
        }

        Prompt prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        return Map.of(
                "promptId", prompt.getId(),
                "copyCount", prompt.getCopyCount()
        );
    }

    @Transactional(readOnly = true)
    public PromptListResponse listMine(long accountId, int page, int size) {
        // ✅ 목록도 정책상 막고 싶으면 여기서 상태 체크(원하면 제거 가능)
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        if (page < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        var result = promptRepository.findByAuthor_IdAndDeletedAtIsNullAndStatusNot(
                accountId,
                PromptStatus.DELETED,
                pageable
        );

        var items = result.getContent()
                .stream()
                .map(p -> PromptResponse.from(p, false, false))
                .toList();

        PageMeta meta = new PageMeta(
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return new PromptListResponse(items, meta);
    }

    /* ===============================
       likes
       =============================== */

    @Transactional
    public LikeResponse like(long accountId, Long promptId) {
        // ✅ 계정 상태 체크
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        // ✅ 멱등 처리: 이미 좋아요면 예외 대신 그대로 성공 처리
        try {
            promptLikeRepository.like(promptId, accountId);
        } catch (DataIntegrityViolationException e) {
            // 이미 좋아요 상태로 보고 통과
        }

        int likeCount = promptRepository.findLikeCountOrZero(promptId);
        return new LikeResponse(true, likeCount);
    }

    @Transactional
    public LikeResponse unlike(long accountId, Long promptId) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        promptLikeRepository.unlike(promptId, accountId);

        int likeCount = promptRepository.findLikeCountOrZero(promptId);
        return new LikeResponse(false, likeCount);
    }

    @Transactional(readOnly = true)
    public PromptListResponse listLiked(long accountId, int page, int size) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        if (page < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        var result = promptRepository.findLikedPrompts(accountId, PromptStatus.DELETED, pageable);

        var items = result.getContent()
                .stream()
                .map(p -> PromptResponse.from(p, true, false))
                .toList();

        PageMeta meta = new PageMeta(
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return new PromptListResponse(items, meta);
    }

    /* ===============================
       bookmarks
       =============================== */

    @Transactional
    public BookmarkResponse bookmark(long accountId, Long promptId) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        // ✅ 멱등 처리
        try {
            promptBookmarkRepository.bookmark(promptId, accountId);
        } catch (DataIntegrityViolationException e) {
            // 이미 북마크 상태로 보고 통과
        }

        int bookmarkCount = promptRepository.findBookmarkCountOrZero(promptId);
        return new BookmarkResponse(true, bookmarkCount);
    }

    @Transactional
    public BookmarkResponse unbookmark(long accountId, Long promptId) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        if (!promptBookmarkRepository.existsActive(promptId, accountId)) {
            int bookmarkCount = promptRepository.findBookmarkCountOrZero(promptId);
            return new BookmarkResponse(false, bookmarkCount);
        }

        promptBookmarkRepository.unbookmark(promptId, accountId);

        int bookmarkCount = promptRepository.findBookmarkCountOrZero(promptId);
        return new BookmarkResponse(false, bookmarkCount);
    }

    @Transactional(readOnly = true)
    public PromptListResponse listBookmarked(long accountId, int page, int size) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        if (page < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");

        long total = promptBookmarkRepository.countActiveByAccount(accountId);
        int totalPages = (total == 0) ? 0 : (int) ((total + size - 1) / size);
        boolean hasNext = page < totalPages;

        int offset = (page - 1) * size;

        List<Long> bookmarkedIds = promptBookmarkRepository.findActiveBookmarkedPromptIds(accountId, size, offset);
        if (bookmarkedIds.isEmpty()) {
            PageMeta meta = new PageMeta(page, size, total, totalPages, false);
            return new PromptListResponse(List.of(), meta);
        }

        List<Prompt> prompts = promptRepository.findByIdInVisibleWithGraph(bookmarkedIds, PromptStatus.DELETED);

        Set<Long> likedPromptIds = new HashSet<>(promptLikeRepository.findActiveLikedPromptIds(accountId, bookmarkedIds));

        Map<Long, Prompt> promptMap = new HashMap<>();
        for (Prompt p : prompts) promptMap.put(p.getId(), p);

        List<PromptResponse> items = new ArrayList<>();
        for (Long id : bookmarkedIds) {
            Prompt p = promptMap.get(id);
            if (p == null) continue;
            boolean isLiked = likedPromptIds.contains(id);
            items.add(PromptResponse.from(p, isLiked, true));
        }

        PageMeta meta = new PageMeta(page, size, total, totalPages, hasNext);
        return new PromptListResponse(items, meta);
    }

    /* ===============================
       reviews
       =============================== */

    @Transactional
    public ReviewResponse createReview(long accountId, Long promptId, ReviewCreateRequest req) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        if (promptReviewRepository.existsActiveByPromptIdAndAccountId(promptId, accountId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 이 프롬프트에 리뷰를 작성했습니다.");
        }

        String content = (req.content() == null) ? "" : req.content().trim();
        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "리뷰 내용은 비어있을 수 없습니다.");
        }

        try {
            PromptReview saved = promptReviewRepository.save(
                    new PromptReview(promptId, accountId, content)
            );
            return ReviewResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 exists 체크 통과 후 insert에서 터질 수 있음
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 이 프롬프트에 리뷰를 작성했습니다.");
        }
    }

    /**
     * ✅ 리뷰 목록 조회 (커서 기반 무한스크롤)
     */
    @Transactional(readOnly = true)
    public ReviewListResponse listReviews(Long promptId, int size, LocalDateTime cursorCreatedAt, Long cursorId) {

        if (size < 1 || size > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");
        }

        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, size + 1);

        List<PromptReview> rows;
        if (cursorCreatedAt == null || cursorId == null) {
            rows = promptReviewRepository.findFirstPage(promptId, pageable);
        } else {
            rows = promptReviewRepository.findNextPage(promptId, cursorCreatedAt, cursorId, pageable);
        }

        boolean hasNext = rows.size() > size;
        if (hasNext) {
            rows = rows.subList(0, size);
        }

        List<ReviewResponse> items = rows.stream()
                .map(ReviewResponse::from)
                .toList();

        String nextCursorCreatedAtStr = null;
        Long nextCursorId = null;

        if (hasNext && !rows.isEmpty()) {
            PromptReview last = rows.get(rows.size() - 1);
            nextCursorCreatedAtStr = last.getCreatedAt().toString();
            nextCursorId = last.getId();
        }

        return new ReviewListResponse(
                items,
                new ReviewListResponse.CursorMeta(
                        size,
                        hasNext,
                        nextCursorCreatedAtStr,
                        nextCursorId
                )
        );
    }

    @Transactional
    public void deleteReview(long accountId, Long promptId, Long reviewId) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        PromptReview review = promptReviewRepository.findByIdAndPromptId(reviewId, promptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        if (review.getDeletedAt() != null) {
            return; // 멱등 성공
        }

        if (!Objects.equals(review.getAccountId(), accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "작성자만 리뷰를 삭제할 수 있습니다.");
        }

        promptReviewRepository.softDeleteById(reviewId, LocalDateTime.now());
    }

    @Transactional
    public ReviewResponse updateReview(long accountId, Long promptId, Long reviewId, ReviewUpdateRequest req) {
        getAccountActiveOrThrow(accountId, "계정을 찾을 수 없습니다.");

        String content = (req.content() == null) ? "" : req.content().trim();
        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "리뷰 내용은 비어있을 수 없습니다.");
        }

        int updated = promptReviewRepository.updateContent(
                reviewId,
                promptId,
                accountId,
                content,
                LocalDateTime.now()
        );

        if (updated == 0) {
            // 존재를 숨기고 싶으면 NOT_FOUND로 통일 (현 정책 유지)
            throw new BusinessException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다.");
        }

        PromptReview refreshed = promptReviewRepository.findActiveByIdAndPromptId(reviewId, promptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        return ReviewResponse.from(refreshed);
    }

    /* ===============================
       helpers
       =============================== */

    private Account getAccountActiveOrThrow(long accountId, String notFoundMessage) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, notFoundMessage));

        LocalDateTime now = LocalDateTime.now();

        if (account.getStatus() == AccountStatus.DELETED) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DELETED,
                    "탈퇴 처리된 계정입니다. 재가입을 진행해 주세요."
            );
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            LocalDateTime until = account.getSuspendedUntil();
            if (until == null || now.isBefore(until)) {
                Object details = (until == null)
                        ? Map.of("suspendedUntil", "INDEFINITE")
                        : Map.of("suspendedUntil", until);

                throw new BusinessException(
                        ErrorCode.ACCOUNT_SUSPENDED,
                        "정지된 계정입니다.",
                        details
                );
            }
        }

        return account;
    }

    private static List<Long> safeIds(List<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(v -> v > 0)
                .distinct()
                .toList();
    }

    private static void validateAllIdsExist(List<Long> requestedIds, int foundSize, String field) {
        if (requestedIds.size() != foundSize) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "존재하지 않거나 삭제된 ID가 포함되어 있습니다.",
                    Map.of(field, requestedIds)
            );
        }
    }
}