package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.PromptLike;
import com.promlog.promlog.prompt.domain.PromptLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptLikeRepository extends JpaRepository<PromptLike, PromptLikeId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO prompt_likes (prompt_id, account_id, deleted_at)
        VALUES (:promptId, :accountId, NULL)
        ON DUPLICATE KEY UPDATE deleted_at = NULL
        """, nativeQuery = true)
    int like(@Param("promptId") Long promptId,
             @Param("accountId") Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE prompt_likes
           SET deleted_at = NOW(3)
         WHERE prompt_id = :promptId
           AND account_id = :accountId
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int unlike(@Param("promptId") Long promptId,
               @Param("accountId") Long accountId);
}
