package com.promlog.promlog.prompt.domain;

import com.promlog.promlog.account.domain.Account;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "prompt_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_prompt_reviews_unique", columnNames = {"prompt_id", "account_id"})
        },
        indexes = {
                @Index(name = "idx_pr_prompt", columnList = "prompt_id"),
                @Index(name = "idx_pr_account", columnList = "account_id"),
                @Index(name = "idx_pr_deleted_at", columnList = "deleted_at")
        }
)
public class PromptReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK 값 조회 편하게 뽑으려고 insertable/updatable=false로 id 컬럼도 같이 둠
    @Column(name = "prompt_id", nullable = false)
    private Long promptId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false, insertable = false, updatable = false)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    private Account account;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected PromptReview() {}

    public PromptReview(Long promptId, Long accountId, String content) {
        this.promptId = promptId;
        this.accountId = accountId;
        this.content = content;
    }

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete(LocalDateTime now) {
        this.deletedAt = now;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getPromptId() { return promptId; }
    public Long getAccountId() { return accountId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
