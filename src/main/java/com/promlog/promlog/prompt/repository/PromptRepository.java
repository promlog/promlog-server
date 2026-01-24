package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long>, JpaSpecificationExecutor<Prompt> {

    @EntityGraph(attributePaths = {
            "author",
            "promptCategories", "promptCategories.category",
            "promptPlatforms", "promptPlatforms.platform"
    })
    Page<Prompt> findByDeletedAtIsNullAndStatusNot(PromptStatus status, Pageable pageable);

    Optional<Prompt> findByIdAndDeletedAtIsNullAndStatusNot(Long id, PromptStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Prompt p
           set p.viewCount = p.viewCount + 1
         where p.id = :id
           and p.deletedAt is null
           and p.status <> 'DELETED'
    """)
    int increaseViewCount(Long id);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Prompt p
           set p.copyCount = p.copyCount + 1
         where p.id = :id
           and p.deletedAt is null
           and p.status <> 'DELETED'
    """)
    int increaseCopyCount(Long id);

    // ✅ 좋아요 카운트 +1
    @Modifying(clearAutomatically = true)
    @Query("""
        update Prompt p
           set p.likeCount = p.likeCount + 1
         where p.id = :id
           and p.deletedAt is null
           and p.status <> 'DELETED'
    """)
    int increaseLikeCount(Long id);

    // ✅ 좋아요 카운트 -1 (0 아래로 안 내려가게)
    @Modifying(clearAutomatically = true)
    @Query("""
        update Prompt p
           set p.likeCount = case when p.likeCount > 0 then p.likeCount - 1 else 0 end
         where p.id = :id
           and p.deletedAt is null
           and p.status <> 'DELETED'
    """)
    int decreaseLikeCount(Long id);

    // ✅ likeCount 조회 (null 방지)
    @Query("""
        select coalesce(p.likeCount, 0)
        from Prompt p
        where p.id = :id
    """)
    int findLikeCountOrZero(Long id);

    @EntityGraph(attributePaths = {
            "author",
            "promptCategories", "promptCategories.category",
            "promptPlatforms", "promptPlatforms.platform"
    })
    Page<Prompt> findByAuthor_IdAndDeletedAtIsNullAndStatusNot(
            Long authorId,
            PromptStatus status,
            Pageable pageable
    );

    @Query("""
        select distinct p
        from Prompt p
        join fetch p.author a
        left join fetch p.promptCategories pc
        left join fetch pc.category
        left join fetch p.promptPlatforms pp
        left join fetch pp.platform
        where p.id = :id
          and p.deletedAt is null
          and p.status <> :status
    """)
    Optional<Prompt> findDetailWithAuthorAndTags(Long id, PromptStatus status);
}
