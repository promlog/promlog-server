package com.promlog.promlog.prompt.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_bookmarks")
public class PromptBookmark {

    @EmbeddedId
    private PromptBookmarkId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected PromptBookmark() {
    }

    public PromptBookmark(PromptBookmarkId id) {
        this.id = id;
    }

    public PromptBookmarkId getId() {
        return id;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
