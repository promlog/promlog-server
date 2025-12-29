package com.promlog.promlog.oauthidentity.repository;

import com.promlog.promlog.oauthidentity.domain.OAuthIdentity;
import com.promlog.promlog.oauthidentity.domain.OAuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;


public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {

    Optional<OAuthIdentity> findByProviderAndSubjectAndDeletedAtIsNull(OAuthProviderType provider, String subject);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OAuthIdentity oi
           set oi.deletedAt = :deletedAt
         where oi.account.id = :accountId
           and oi.deletedAt is null
    """)
    int softDeleteAllByAccountId(@Param("accountId") long accountId,
                                 @Param("deletedAt") LocalDateTime deletedAt);
}

