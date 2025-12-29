package com.promlog.promlog.account.repository;

import com.promlog.promlog.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}