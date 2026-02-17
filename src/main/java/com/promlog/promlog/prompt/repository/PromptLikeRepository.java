package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.PromptLike;
import com.promlog.promlog.prompt.domain.PromptLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromptLikeRepository extends JpaRepository<PromptLike, PromptLikeId> {

    // ✅ (이미 너가 쓰고 있는 메서드들) - 시그니처는 기존 그대로 유지
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO prompt_likes (prompt_id, account_id, created_at, updated_at, deleted_at)
        VALUES (:promptId, :accountId, NOW(3), NOW(3), NULL)
        ON DUPLICATE KEY UPDATE
            deleted_at = NULL,
            updated_at = NOW(3)
        """, nativeQuery = true)
    int like(@Param("promptId") long promptId, @Param("accountId") long accountId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE prompt_likes
           SET deleted_at = NOW(3),
               updated_at = NOW(3)
         WHERE prompt_id = :promptId
           AND account_id = :accountId
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int unlike(@Param("promptId") long promptId, @Param("accountId") long accountId);

    // ✅ 단건: detail에서 사용
    @Query("""
        select (count(pl) > 0)
          from PromptLike pl
         where pl.id.promptId = :promptId
           and pl.id.accountId = :accountId
           and pl.deletedAt is null
    """)
    boolean existsActive(@Param("promptId") long promptId, @Param("accountId") long accountId);

    // ✅ 다건: list에서 사용 (현재 페이지 promptIds 대상으로 한 번에 조회)
    @Query("""
        select pl.id.promptId
          from PromptLike pl
         where pl.id.accountId = :accountId
           and pl.id.promptId in :promptIds
           and pl.deletedAt is null
    """)
    List<Long> findActiveLikedPromptIds(
            @Param("accountId") long accountId,
            @Param("promptIds") List<Long> promptIds
    );
}
