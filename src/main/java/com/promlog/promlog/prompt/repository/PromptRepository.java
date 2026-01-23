package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {

    // ✅ 목록 조회: author까지 같이 로딩 (nickname 필요)
    @EntityGraph(attributePaths = "author")
    Page<Prompt> findByDeletedAtIsNullAndStatusNot(PromptStatus status, Pageable pageable);

    // ✅ 상세 기본 조회(혹시 다른 곳에서 쓰면 author는 LAZY라 nickname 필요 시 문제)
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

    // ✅ 내가 쓴 프롬프트 목록: author까지 같이 로딩
    @EntityGraph(attributePaths = "author")
    Page<Prompt> findByAuthor_IdAndDeletedAtIsNullAndStatusNot(
            Long authorId,
            PromptStatus status,
            Pageable pageable
    );

    // ✅ 상세 조회: fetch join으로 author까지 같이 가져오기 (조회수 증가 후 조회에 사용)
    @Query("""
        select p
        from Prompt p
        join fetch p.author a
        where p.id = :id
          and p.deletedAt is null
          and p.status <> :status
    """)
    Optional<Prompt> findDetailWithAuthor(Long id, PromptStatus status);
}