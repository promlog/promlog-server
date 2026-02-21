package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.PromptReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // ✅ 삭제용: promptId까지 같이 검증해서 찾아오기
    @Query("""
        select r
          from PromptReview r
         where r.id = :reviewId
           and r.promptId = :promptId
    """)
    Optional<PromptReview> findByIdAndPromptId(@Param("reviewId") Long reviewId,
                                               @Param("promptId") Long promptId);

    // ✅ 멱등 soft delete
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PromptReview r
           set r.deletedAt = :now
         where r.id = :reviewId
           and r.deletedAt is null
    """)
    int softDeleteById(@Param("reviewId") Long reviewId,
                       @Param("now") LocalDateTime now);

    // ✅ 수정용 update (작성자 + 미삭제 조건)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PromptReview r
           set r.content = :content,
               r.updatedAt = :now
         where r.id = :reviewId
           and r.promptId = :promptId
           and r.accountId = :accountId
           and r.deletedAt is null
    """)
    int updateContent(@Param("reviewId") Long reviewId,
                      @Param("promptId") Long promptId,
                      @Param("accountId") Long accountId,
                      @Param("content") String content,
                      @Param("now") LocalDateTime now);

    // ✅ 수정 후 응답 내려주려고 재조회
    @Query("""
        select r
          from PromptReview r
         where r.id = :reviewId
           and r.promptId = :promptId
           and r.deletedAt is null
    """)
    Optional<PromptReview> findActiveByIdAndPromptId(@Param("reviewId") Long reviewId,
                                                     @Param("promptId") Long promptId);
}