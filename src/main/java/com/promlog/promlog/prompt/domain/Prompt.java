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

    public void update(String title, String body, String sourceUrl, Boolean isAnonymous) {
        if (title != null) this.title = title.trim();
        if (body != null) this.body = body;
        // sourceUrl은 null이 들어오면 "지우기" 의도일 수 있음 -> 요청에 들어온 경우에만 반영해야 함
        // 그래서 서비스에서 "요청에 sourceUrl 키가 있었는지"를 구분해 처리할 건데,
        // 단순 버전(W1)은: sourceUrl 필드가 null이면 지우기라고 해석 (요구사항에 sourceUrl:null 예시가 있음)
        this.sourceUrl = sourceUrl;

        if (isAnonymous != null) this.isAnonymous = isAnonymous;
    }

    public void softDelete(LocalDateTime now) {
        this.status = PromptStatus.DELETED;
        this.deletedAt = now;
    }
}
