package com.promlog.promlog.prompt.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_likes")
public class PromptLike {

    @EmbeddedId
    private PromptLikeId id;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected PromptLike() {}

    public PromptLike(PromptLikeId id) {
        this.id = id;
    }

    public PromptLikeId getId() { return id; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
