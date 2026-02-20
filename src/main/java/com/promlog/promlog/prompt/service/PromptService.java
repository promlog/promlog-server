package com.promlog.promlog.prompt.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.response.PageMeta;
import com.promlog.promlog.prompt.category.repository.CategoryRepository;
import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;
import com.promlog.promlog.prompt.dto.BookmarkResponse;
import com.promlog.promlog.prompt.dto.LikeResponse;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptListResponse;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.dto.PromptUpdateRequest;
import com.promlog.promlog.prompt.platform.repository.PlatformRepository;
import com.promlog.promlog.prompt.repository.PromptBookmarkRepository;
import com.promlog.promlog.prompt.repository.PromptLikeRepository;
import com.promlog.promlog.prompt.repository.PromptRepository;
import com.promlog.promlog.prompt.repository.PromptSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final PlatformRepository platformRepository;

    public PromptService(
            PromptRepository promptRepository,
            PromptLikeRepository promptLikeRepository,
            PromptBookmarkRepository promptBookmarkRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            PlatformRepository platformRepository
    ) {
        this.promptRepository = promptRepository;
        this.promptLikeRepository = promptLikeRepository;
        this.promptBookmarkRepository = promptBookmarkRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.platformRepository = platformRepository;
    }

    @Transactional
    public PromptResponse create(long accountId, PromptCreateRequest req) {

        Account author = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "작성자 계정을 찾을 수 없습니다."));

        Prompt prompt = new Prompt(
                author,
                req.title().trim(),
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

        // ✅ isLiked
        final Set<Long> likedPromptIds =
                (viewerAccountId == null || promptIds.isEmpty())
                        ? Collections.emptySet()
                        : new HashSet<>(promptLikeRepository.findActiveLikedPromptIds(viewerAccountId, promptIds));

        // ✅ isBookmarked
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

        var prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        if (!prompt.getAuthorAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "작성자만 수정할 수 있습니다.");
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

        return PromptResponse.from(refreshed, false, false);
    }

    @Transactional
    public void delete(long accountId, Long promptId) {
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
        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        promptLikeRepository.like(promptId, accountId);

        int likeCount = promptRepository.findLikeCountOrZero(promptId);
        return new LikeResponse(true, likeCount);
    }

    @Transactional
    public LikeResponse unlike(long accountId, Long promptId) {
        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        promptLikeRepository.unlike(promptId, accountId);

        int likeCount = promptRepository.findLikeCountOrZero(promptId);
        return new LikeResponse(false, likeCount);
    }

    @Transactional(readOnly = true)
    public PromptListResponse listLiked(long accountId, int page, int size) {

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
        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        promptBookmarkRepository.bookmark(promptId, accountId);

        int bookmarkCount = promptRepository.findBookmarkCountOrZero(promptId);
        return new BookmarkResponse(true, bookmarkCount);
    }

    @Transactional
    public BookmarkResponse unbookmark(long accountId, Long promptId) {
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
            items.add(PromptResponse.from(p, isLiked, true)); // ✅ 북마크 목록이니까 true
        }

        PageMeta meta = new PageMeta(page, size, total, totalPages, hasNext);
        return new PromptListResponse(items, meta);
    }

    /* ===============================
       helpers
       =============================== */

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
