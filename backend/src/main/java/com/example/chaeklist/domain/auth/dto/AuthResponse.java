package com.example.chaeklist.domain.auth.dto;

import com.example.chaeklist.domain.auth.entity.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 성공 응답")
public record AuthResponse(
		@Schema(description = "사용자 ID", example = "1")
		long id,
		@Schema(description = "이메일", example = "reader@chaeklist.kr")
		String email,
		@Schema(description = "닉네임", example = "quiet-reader")
		String nickname,
		@Schema(description = "계정 상태", example = "ACTIVE")
		String status,
		@Schema(description = "사용자 역할", example = "USER")
		String role,
		@Schema(description = "토큰 타입", example = "Bearer")
		String tokenType,
		@Schema(description = "API 인증에 사용하는 access token")
		String accessToken,
		@Schema(description = "토큰 재발급에 사용할 refresh token")
		String refreshToken
) {

	public static AuthResponse from(UserAccount user, TokenPair tokens) {
		return new AuthResponse(
				user.id(),
				user.email(),
				user.nickname(),
				user.status(),
				user.role(),
				"Bearer",
				tokens.accessToken(),
				tokens.refreshToken()
		);
	}
}
