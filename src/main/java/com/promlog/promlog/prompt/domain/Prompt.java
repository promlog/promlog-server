package com.promlog.promlog.prompt.domain;

import com.promlog.promlog.account.domain.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(
        name = "prompts",
        indexes = {
                @Index(name = "idx_prompts_author", columnList = "author_account_id"),
                @Index(name = "idx_prompts_status_created", columnList = "status, created_at"),
                @Index(name = "idx_prompts_like_sort", columnList = "status, like_count, created_at")
        }
)
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ===============================
       작성자 (accounts.id FK)
       =============================== */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_account_id", nullable = false)
    private Account author;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromptStatus status = PromptStatus.ACTIVE;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount = 0;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "copy_count", nullable = false)
    private long copyCount = 0;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.valueOf(0.00);

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ===============================
       생성자
       =============================== */
    public Prompt(
            Account author,
            String title,
            String body,
            String sourceUrl,
            boolean isAnonymous
    ) {
        this.author = author;
        this.title = title;
        this.body = body;
        this.sourceUrl = sourceUrl;
        this.isAnonymous = isAnonymous;
        this.status = PromptStatus.ACTIVE;
    }

    /* ===============================
       JPA Lifecycle
       =============================== */
    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* ===============================
       편의 메서드
       =============================== */
    public Long getAuthorAccountId() {
        return author.getId();
    }
}
