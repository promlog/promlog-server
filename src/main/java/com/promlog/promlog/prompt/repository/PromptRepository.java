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
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("""
        select p
          from Prompt p
          join PromptLike pl
            on pl.id.promptId = p.id
         where pl.id.accountId = :accountId
           and pl.deletedAt is null
           and p.deletedAt is null
           and p.status <> :deletedStatus
    """)
    Page<Prompt> findLikedPrompts(
            @Param("accountId") long accountId,
            @Param("deletedStatus") PromptStatus deletedStatus,
            Pageable pageable
    );

    @Query("""
        select coalesce(p.bookmarkCount, 0)
        from Prompt p
        where p.id = :id
    """)
    int findBookmarkCountOrZero(Long id);

    // ✅ 북마크 목록 조회용: id 리스트로 한 번에 가져오되 author/tags까지 같이
    @EntityGraph(attributePaths = {
            "author",
            "promptCategories", "promptCategories.category",
            "promptPlatforms", "promptPlatforms.platform"
    })
    @Query("""
        select p
        from Prompt p
        where p.id in :ids
          and p.deletedAt is null
          and p.status <> :deletedStatus
    """)
    List<Prompt> findByIdInVisibleWithGraph(@Param("ids") List<Long> ids,
                                            @Param("deletedStatus") PromptStatus deletedStatus);
}
