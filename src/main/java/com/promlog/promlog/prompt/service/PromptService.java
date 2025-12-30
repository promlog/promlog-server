package com.promlog.promlog.prompt.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.dto.PromptUpdateRequest;
import com.promlog.promlog.prompt.repository.PromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.promlog.promlog.global.response.PageMeta;
import com.promlog.promlog.prompt.domain.PromptStatus;
import com.promlog.promlog.prompt.dto.PromptListResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final AccountRepository accountRepository;

    public PromptService(
            PromptRepository promptRepository,
            AccountRepository accountRepository
    ) {
        this.promptRepository = promptRepository;
        this.accountRepository = accountRepository;
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
                req.body(),
                req.sourceUrl(),
                req.isAnonymous()
        );

        Prompt saved = promptRepository.save(prompt);
        return PromptResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PromptListResponse list(String sort, int page, int size) {

        // ✅ page: 1-base, size: 기본 20 / max 50
        if (page < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 1 이상이어야 합니다.");
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1~50 이어야 합니다.");

        String s = (sort == null || sort.isBlank()) ? "latest" : sort;

        Sort springSort;
        if ("latest".equalsIgnoreCase(s)) {
            springSort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 sort 입니다.", java.util.Map.of("sort", s));
        }

        // 0-base로 변환
        PageRequest pageable = PageRequest.of(page - 1, size, springSort);

        var result = promptRepository.findByDeletedAtIsNullAndStatusNot(PromptStatus.DELETED, pageable);

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

        // 1) 조회수 증가 (존재 + 노출 대상일 때만 증가)
        int updated = promptRepository.increaseViewCount(promptId);
        if (updated == 0) {
            // 존재하지 않거나 삭제된 경우
            throw new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다.");
        }

        // 2) 상세 조회
        var prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다.")
                );

        return PromptResponse.from(prompt);
    }

    @Transactional
    public PromptResponse update(long accountId, Long promptId, PromptUpdateRequest req) {
        var prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        // ✅ 작성자 검증
        if (!prompt.getAuthorAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "작성자만 수정할 수 있습니다.");
        }

        // ✅ 부분 수정 적용
        prompt.update(req.title(), req.body(), req.sourceUrl(), req.isAnonymous());

        return PromptResponse.from(prompt);
    }

    @Transactional
    public void delete(long accountId, Long promptId) {
        var prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        // ✅ 작성자만
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

        // 최신 copyCount 조회
        Prompt prompt = promptRepository
                .findByIdAndDeletedAtIsNullAndStatusNot(promptId, PromptStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프롬프트를 찾을 수 없습니다."));

        return Map.of(
                "promptId", prompt.getId(),
                "copyCount", prompt.getCopyCount()
        );
    }
}
