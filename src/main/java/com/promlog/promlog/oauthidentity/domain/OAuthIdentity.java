package com.promlog.promlog.oauthidentity.domain;

import com.promlog.promlog.account.domain.Account;
import jakarta.persistence.*;
        import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_identities",
        indexes = {
                @Index(name = "idx_oauth_provider_subject", columnList = "provider,subject"),
                @Index(name = "idx_oauth_account_id", columnList = "account_id")
        }
)
public class OAuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProviderType provider;

    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    @Column(name = "email", length = 255)
    private String email;

    // MVP: JSON 컬럼을 문자열로 보관해도 충분
    @Column(name = "profile_json", columnDefinition = "json")
    private String profileJson;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected OAuthIdentity() {}

    public OAuthIdentity(Account account, OAuthProviderType provider, String subject, String email, String profileJson) {
        this.account = account;
        this.provider = provider;
        this.subject = subject;
        this.email = email;
        this.profileJson = profileJson;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public OAuthProviderType getProvider() { return provider; }
    public String getSubject() { return subject; }
    public String getEmail() { return email; }
    public String getProfileJson() { return profileJson; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void softDelete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
