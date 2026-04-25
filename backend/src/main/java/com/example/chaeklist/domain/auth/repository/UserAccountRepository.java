package com.example.chaeklist.domain.auth.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.chaeklist.domain.auth.entity.UserAccount;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {

	private final AtomicLong nextId = new AtomicLong(1);
	private final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();
	private final Map<String, Long> userIdsByNickname = new ConcurrentHashMap<>();

	public boolean existsByEmail(String email) {
		return usersByEmail.containsKey(email);
	}

	public boolean existsByNickname(String nickname) {
		return userIdsByNickname.containsKey(nickname);
	}

	public Optional<UserAccount> findByEmail(String email) {
		return Optional.ofNullable(usersByEmail.get(email));
	}

	public UserAccount save(UserAccount user) {
		usersByEmail.put(user.email(), user);
		userIdsByNickname.put(user.nickname(), user.id());
		return user;
	}

	public long nextId() {
		return nextId.getAndIncrement();
	}
}
