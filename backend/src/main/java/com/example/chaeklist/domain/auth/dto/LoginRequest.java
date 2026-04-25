package com.example.chaeklist.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 요청")
public record LoginRequest(
		@Schema(description = "이메일", example = "reader@chaeklist.kr")
		String email,
		@Schema(description = "비밀번호", example = "chaeklist123")
		String password
) {
}
