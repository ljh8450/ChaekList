package com.example.chaeklist.domain.auth.entity;

import java.time.LocalDateTime;

public record UserAccount(
		long id,
		String email,
		String nickname,
		String passwordHash,
		String status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
