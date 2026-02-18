package com.promlog.promlog.prompt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PromptBookmarkId implements Serializable {

    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "account_id")
    private Long accountId;

    protected PromptBookmarkId() {
    }

    public PromptBookmarkId(Long promptId, Long accountId) {
        this.promptId = promptId;
        this.accountId = accountId;
    }

    public Long getPromptId() {
        return promptId;
    }

    public Long getAccountId() {
        return accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptBookmarkId that)) return false;
        return Objects.equals(promptId, that.promptId) &&
                Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptId, accountId);
    }
}
