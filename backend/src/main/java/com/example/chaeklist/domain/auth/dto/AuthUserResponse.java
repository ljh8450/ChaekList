package com.example.chaeklist.domain.auth.dto;

import com.example.chaeklist.global.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 사용자 응답")
public record AuthUserResponse(
		@Schema(description = "사용자 ID", example = "1")
		long id,
		@Schema(description = "이메일", example = "reader@chaeklist.kr")
		String email,
		@Schema(description = "닉네임", example = "quiet-reader")
		String nickname,
		@Schema(description = "계정 상태", example = "ACTIVE")
		String status,
		@Schema(description = "사용자 역할", example = "USER")
		String role
) {

	public static AuthUserResponse from(AuthenticatedUser user) {
		return new AuthUserResponse(user.id(), user.email(), user.nickname(), user.status(), user.role());
	}
}
