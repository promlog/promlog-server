package com.promlog.promlog.prompt.repository;

import com.promlog.promlog.prompt.domain.PromptReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptReviewRepository extends JpaRepository<PromptReview, Long> {

    boolean existsByPromptIdAndAccountIdAndDeletedAtIsNull(Long promptId, Long accountId);

    Optional<PromptReview> findByPromptIdAndAccountIdAndDeletedAtIsNull(Long promptId, Long accountId);
}
