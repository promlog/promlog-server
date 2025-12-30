package com.promlog.promlog.prompt.service;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.dto.PromptCreateRequest;
import com.promlog.promlog.prompt.dto.PromptResponse;
import com.promlog.promlog.prompt.repository.PromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
