package com.example.chaeklist;

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
