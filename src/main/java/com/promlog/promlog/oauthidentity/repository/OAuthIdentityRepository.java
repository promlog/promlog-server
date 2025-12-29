package com.promlog.promlog.oauthidentity.repository;

import com.promlog.promlog.oauthidentity.domain.OAuthIdentity;
import com.promlog.promlog.oauthidentity.domain.OAuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {

    Optional<OAuthIdentity> findByProviderAndSubjectAndDeletedAtIsNull(OAuthProviderType provider, String subject);
}