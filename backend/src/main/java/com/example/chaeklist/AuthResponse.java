package com.example.chaeklist;

public record AuthResponse(
		long id,
		String email,
		String nickname,
		String status
) {

	static AuthResponse from(UserAccount user) {
		return new AuthResponse(user.id(), user.email(), user.nickname(), user.status());
	}
}
