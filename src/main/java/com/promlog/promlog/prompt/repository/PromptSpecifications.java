package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class PromptSpecifications {

    private PromptSpecifications() {}

    public static Specification<Prompt> baseVisible() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.notEqual(root.get("status"), PromptStatus.DELETED)
        );
    }

    /**
     * categoryIds 중 하나라도 포함하면 매칭(IN)
     */
    public static Specification<Prompt> hasAnyCategoryIds(List<Long> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) return cb.conjunction();

            // DISTINCT 안 하면 join 때문에 Prompt가 중복 row로 나올 수 있음
            query.distinct(true);

            // root -> promptCategories -> category
            var pc = root.join("promptCategories", JoinType.INNER);
            var c = pc.join("category", JoinType.INNER);

            return c.get("id").in(categoryIds);
        };
    }

    /**
     * platformIds 중 하나라도 포함하면 매칭(IN)
     */
    public static Specification<Prompt> hasAnyPlatformIds(List<Long> platformIds) {
        return (root, query, cb) -> {
            if (platformIds == null || platformIds.isEmpty()) return cb.conjunction();

            query.distinct(true);

            // root -> promptPlatforms -> platform
            var pp = root.join("promptPlatforms", JoinType.INNER);
            var p = pp.join("platform", JoinType.INNER);

            return p.get("id").in(platformIds);
        };
    }
}
