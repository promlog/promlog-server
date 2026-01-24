package com.promlog.promlog.prompt.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.response.PageMeta;
import com.promlog.promlog.prompt.category.repository.CategoryRepository;
import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;
import com.promlog.promlog.prompt.dto.LikeResponse;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptListResponse;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.dto.PromptUpdateRequest;
import com.promlog.promlog.prompt.platform.repository.PlatformRepository;
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
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final PlatformRepository platformRepository;

    public PromptService(
            PromptRepository promptRepository,
            PromptLikeRepository promptLikeRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            PlatformRepository platformRepository
    ) {
        this.promptRepository = promptRepository;
        this.promptLikeRepository = promptLikeRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.platformRepository = platformRepository;
    }

    @Transactional
    public PromptResponse create(long accountId, PromptCreateRequest req) {

        Account author = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.NOT_FOUND,
                                "작성자 계정을 찾을 수 없습니다."
                        )
                );

        Prompt prompt = new Prompt(
                author,
                req.title().trim(),
                req.description(),
                req.prompt(),
                req.tip(),
                req.sourceUrl(),
                req.isAnonymous()
        );

        Prompt saved = promptRepository.save(prompt); // id 생성

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

        return PromptResponse.from(refreshed);
    }

    @Transactional(readOnly = true)
    public PromptListResponse list(String sort, int page, int size,
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

        List<PromptResponse> items = result.getContent()
                .stream()
                .map(PromptResponse::from)
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
    public PromptResponse getDetail(Long promptId) {

        int updated = promptRepository.increaseViewCount(promptId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다.");
        }

        var prompt = promptRepository
                .findDetailWithAuthorAndTags(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        return PromptResponse.from(prompt);
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

        return PromptResponse.from(refreshed);
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

        if (page < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        }
        if (size < 1 || size > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");
        }

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
                .map(PromptResponse::from)
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

        int affected = promptLikeRepository.like(promptId, accountId);

        if (affected > 0) {
            promptRepository.increaseLikeCount(promptId);
        }

        int likeCount = promptRepository.findLikeCountOrZero(promptId);
        return new LikeResponse(true, likeCount);
    }

    @Transactional
    public LikeResponse unlike(long accountId, Long promptId) {
        promptRepository.findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        int affected = promptLikeRepository.unlike(promptId, accountId);

        if (affected == 1) {
            promptRepository.decreaseLikeCount(promptId);
        }

        int likeCount = promptRepository.findLikeCountOrZero(promptId);
        return new LikeResponse(false, likeCount);
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

    @Transactional(readOnly = true)
    public PromptListResponse listLiked(long accountId, int page, int size) {

        if (page < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        }
        if (size < 1 || size > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");
        }

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt") // 좋아요 “최신순”
        );

        var result = promptRepository.findLikedPrompts(accountId, PromptStatus.DELETED, pageable);

        var items = result.getContent()
                .stream()
                .map(PromptResponse::from)
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

}
