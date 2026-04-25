package com.example.chaeklist.domain.auth.repository;

import java.util.Optional;

import com.example.chaeklist.domain.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<UserAccount> findByEmail(String email);
}
