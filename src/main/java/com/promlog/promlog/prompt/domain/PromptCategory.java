package com.promlog.promlog.prompt.domain;

import com.promlog.promlog.prompt.category.domain.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "prompt_categories")
public class PromptCategory {

    @EmbeddedId
    private PromptCategoryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("promptId")
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PromptCategory(Prompt prompt, Category category) {
        this.prompt = prompt;
        this.category = category;
        this.id = new PromptCategoryId(prompt.getId(), category.getId());
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
