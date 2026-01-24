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
public class PromptCategoryId implements Serializable {

    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "category_id")
    private Long categoryId;

    public PromptCategoryId(Long promptId, Long categoryId) {
        this.promptId = promptId;
        this.categoryId = categoryId;
    }
}
