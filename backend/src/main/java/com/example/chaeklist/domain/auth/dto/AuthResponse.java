package com.example.chaeklist.domain.auth.dto;

import com.example.chaeklist.domain.auth.entity.UserAccount;

public record AuthResponse(
		long id,
		String email,
		String nickname,
		String status,
		String tokenType,
		String accessToken,
		String refreshToken
) {

	public static AuthResponse from(UserAccount user, TokenPair tokens) {
		return new AuthResponse(
				user.id(),
				user.email(),
				user.nickname(),
				user.status(),
				"Bearer",
				tokens.accessToken(),
				tokens.refreshToken()
		);
	}
}
