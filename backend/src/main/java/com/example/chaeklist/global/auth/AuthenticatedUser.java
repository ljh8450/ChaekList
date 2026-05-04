package com.example.chaeklist.global.auth;

public record AuthenticatedUser(
		long id,
		String email,
		String nickname,
		String status,
		String role
) {
}
