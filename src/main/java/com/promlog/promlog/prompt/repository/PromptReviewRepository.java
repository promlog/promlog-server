package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.PromptReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromptReviewRepository extends JpaRepository<PromptReview, Long> {

    @Query("""
        select count(r) > 0
          from PromptReview r
         where r.promptId = :promptId
           and r.accountId = :accountId
           and r.deletedAt is null
    """)
    boolean existsActiveByPromptIdAndAccountId(@Param("promptId") Long promptId,
                                               @Param("accountId") Long accountId);

    // ✅ 첫 페이지(커서 없음): 최신순
    @Query("""
        select r
          from PromptReview r
         where r.promptId = :promptId
           and r.deletedAt is null
         order by r.createdAt desc, r.id desc
    """)
    List<PromptReview> findFirstPage(@Param("promptId") Long promptId, Pageable pageable);

    // ✅ 다음 페이지(커서 있음): (createdAt, id) 기준으로 "더 이전" 것들
    @Query("""
        select r
          from PromptReview r
         where r.promptId = :promptId
           and r.deletedAt is null
           and (
                r.createdAt < :cursorCreatedAt
             or (r.createdAt = :cursorCreatedAt and r.id < :cursorId)
           )
         order by r.createdAt desc, r.id desc
    """)
    List<PromptReview> findNextPage(@Param("promptId") Long promptId,
                                    @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                    @Param("cursorId") Long cursorId,
                                    Pageable pageable);
}