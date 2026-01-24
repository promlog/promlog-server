package com.promlog.promlog.prompt.domain;

import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.prompt.category.domain.Category;
import com.promlog.promlog.prompt.platform.domain.Platform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /* ===============================
       본문 분리: 설명 / 프롬프트 / 팁(선택)
       =============================== */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 설명

    @Lob
    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String prompt; // 프롬프트 본문

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tip; // 선택

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
       카테고리/플랫폼 매핑
       - orphanRemoval=true 로 "교체" 처리 가능
       =============================== */
    @OneToMany(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PromptCategory> promptCategories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PromptPlatform> promptPlatforms = new LinkedHashSet<>();

    /* ===============================
       생성자
       =============================== */
    public Prompt(
            Account author,
            String title,
            String description,
            String prompt,
            String tip,
            String sourceUrl,
            boolean isAnonymous
    ) {
        this.author = author;
        this.title = title;
        this.description = description;
        this.prompt = prompt;
        this.tip = tip;
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

    public void update(String title,
                       String description,
                       String prompt,
                       String tip,
                       String sourceUrl,
                       Boolean isAnonymous) {

        if (title != null) this.title = title.trim();
        if (description != null) this.description = description;
        if (prompt != null) this.prompt = prompt;

        // tip은 null이 들어오면 "지우기" 의도일 수 있음 -> 현재 버전은 그대로 반영(=null이면 삭제)
        this.tip = tip;

        // sourceUrl도 동일하게 null이면 삭제로 처리(요구사항에 따라 키 존재여부 구분 가능)
        this.sourceUrl = sourceUrl;

        if (isAnonymous != null) this.isAnonymous = isAnonymous;
    }

    public void softDelete(LocalDateTime now) {
        this.status = PromptStatus.DELETED;
        this.deletedAt = now;
    }

    /* ===============================
       카테고리/플랫폼 교체
       - Update 정책: null이면 유지, []면 전부 제거, 값 있으면 교체
       - Create는 save 후(=id 생성 후) 호출 권장
       =============================== */
    public void replaceCategories(List<Category> categories) {
        this.promptCategories.clear();
        for (Category c : categories) {
            this.promptCategories.add(new PromptCategory(this, c));
        }
    }

    public void replacePlatforms(List<Platform> platforms) {
        this.promptPlatforms.clear();
        for (Platform p : platforms) {
            this.promptPlatforms.add(new PromptPlatform(this, p));
        }
    }
}
