package com.promlog.promlog.account.domain;

import jakarta.persistence.*;
        import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AccountRole role = AccountRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Account() {}

    public Account(String nickname) {
        this.nickname = nickname;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public AccountRole getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public LocalDateTime getSuspendedUntil() { return suspendedUntil; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void updateLastLoginAt(LocalDateTime now) {
        this.lastLoginAt = now;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void softDelete(LocalDateTime now) {
        this.status = AccountStatus.DELETED;
        this.deletedAt = now;
    }
}
