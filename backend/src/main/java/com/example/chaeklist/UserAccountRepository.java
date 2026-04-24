package com.example.chaeklist;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<UserAccountEntity> findByEmail(String email);
}
