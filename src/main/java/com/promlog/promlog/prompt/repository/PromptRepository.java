package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.Prompt;
import com.promlog.promlog.prompt.domain.PromptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
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

    // 내가 쓴 프롬프트 목록
    Page<Prompt> findByAuthor_IdAndDeletedAtIsNullAndStatusNot(
            Long authorId,
            PromptStatus status,
            Pageable pageable
    );
}