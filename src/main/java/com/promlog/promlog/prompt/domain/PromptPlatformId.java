package com.promlog.promlog.prompt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@EqualsAndHashCode
@Embeddable
public class PromptPlatformId implements Serializable {

    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "platform_id")
    private Long platformId;

    public PromptPlatformId(Long promptId, Long platformId) {
        this.promptId = promptId;
        this.platformId = platformId;
    }
}
