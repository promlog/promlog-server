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

    // ✅ 목록: author + tags까지 같이 로딩 (categories/platforms 응답에 필요)
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

    // ✅ 내 글 목록: author + tags
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

    // ✅ 상세: author + tags fetch join (viewCount 증가 후 조회에 사용)
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
