package com.promlog.promlog.prompt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PromptBookmarkRepository extends JpaRepository<PromptBookmarkIdOnly, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO prompt_bookmarks (prompt_id, account_id, created_at, updated_at, deleted_at)
        VALUES (:promptId, :accountId, NOW(3), NOW(3), NULL)
        ON DUPLICATE KEY UPDATE
          deleted_at = NULL,
          updated_at = NOW(3)
        """, nativeQuery = true)
    void bookmark(Long promptId, Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE prompt_bookmarks
           SET deleted_at = NOW(3),
               updated_at = NOW(3)
         WHERE prompt_id = :promptId
           AND account_id = :accountId
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int unbookmark(Long promptId, Long accountId);

    // ✅ EXISTS는 0/1로 내려올 수 있으니 int로 받는다 (ClassCastException 방지)
    @Query(value = """
        SELECT EXISTS(
          SELECT 1
          FROM prompt_bookmarks
          WHERE prompt_id = :promptId
            AND account_id = :accountId
            AND deleted_at IS NULL
        )
        """, nativeQuery = true)
    int existsActiveRaw(Long promptId, Long accountId);

    default boolean existsActive(Long promptId, Long accountId) {
        return existsActiveRaw(promptId, accountId) == 1;
    }

    // ✅ 내 북마크 목록: prompt_id만 페이징으로 뽑기 (최신순)
    @Query(value = """
        SELECT pb.prompt_id
        FROM prompt_bookmarks pb
        WHERE pb.account_id = :accountId
          AND pb.deleted_at IS NULL
        ORDER BY pb.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Long> findActiveBookmarkedPromptIds(Long accountId, int limit, int offset);

    // ✅ 내 북마크 총 개수
    @Query(value = """
        SELECT COUNT(*)
        FROM prompt_bookmarks pb
        WHERE pb.account_id = :accountId
          AND pb.deleted_at IS NULL
        """, nativeQuery = true)
    long countActiveByAccount(Long accountId);

    // ✅ 목록 조회에서 필요한 "현재 페이지 promptIds 중 내가 북마크한 것"만 뽑기
    @Query(value = """
        SELECT pb.prompt_id
        FROM prompt_bookmarks pb
        WHERE pb.account_id = :accountId
          AND pb.deleted_at IS NULL
          AND pb.prompt_id IN (:promptIds)
        """, nativeQuery = true)
    List<Long> findActiveBookmarkedPromptIdsIn(Long accountId, List<Long> promptIds);
}

/**
 * 주의:
 * JpaRepository는 엔티티 타입이 필요해서 "임시 더미 엔티티"를 하나 둡니다.
 * 실제로 이 리포지토리는 nativeQuery만 쓸 거라 영향 거의 없어요.
 */
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "prompt_bookmarks")
@jakarta.persistence.IdClass(PromptBookmarkKey.class)
class PromptBookmarkIdOnly {

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "prompt_id")
    private Long promptId;

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "account_id")
    private Long accountId;
}

class PromptBookmarkKey implements java.io.Serializable {
    private Long promptId;
    private Long accountId;

    public PromptBookmarkKey() {}

    public PromptBookmarkKey(Long promptId, Long accountId) {
        this.promptId = promptId;
        this.accountId = accountId;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptBookmarkKey k)) return false;
        return java.util.Objects.equals(promptId, k.promptId)
                && java.util.Objects.equals(accountId, k.accountId);
    }

    @Override public int hashCode() {
        return java.util.Objects.hash(promptId, accountId);
    }
}
