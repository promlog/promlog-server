package com.promlog.promlog.prompt.domain;

import com.promlog.promlog.prompt.platform.domain.Platform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "prompt_platforms")
public class PromptPlatform {

    @EmbeddedId
    private PromptPlatformId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("promptId")
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("platformId")
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PromptPlatform(Prompt prompt, Platform platform) {
        this.prompt = prompt;
        this.platform = platform;
        this.id = new PromptPlatformId(prompt.getId(), platform.getId());
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
