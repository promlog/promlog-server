package com.promlog.promlog.prompt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    @Query(value = """
        SELECT EXISTS(
          SELECT 1
          FROM prompt_bookmarks
          WHERE prompt_id = :promptId
            AND account_id = :accountId
            AND deleted_at IS NULL
        )
        """, nativeQuery = true)
    boolean existsActive(Long promptId, Long accountId);
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
