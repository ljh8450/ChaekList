package com.example.chaeklist.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public record SignupRequest(
		@Schema(description = "이메일", example = "new-reader@chaeklist.kr")
		String email,
		@Schema(description = "닉네임", example = "new-reader")
		String nickname,
		@Schema(description = "비밀번호. 8자 이상", example = "chaeklist123")
		String password
) {
}
