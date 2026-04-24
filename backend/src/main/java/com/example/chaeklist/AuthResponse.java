package com.example.chaeklist;

public record AuthResponse(
		long id,
		String email,
		String nickname,
		String status
) {

	static AuthResponse from(UserAccountEntity user) {
		return new AuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getStatus());
	}
}
