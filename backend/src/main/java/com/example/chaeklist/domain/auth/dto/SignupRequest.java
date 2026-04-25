package com.example.chaeklist.domain.auth.dto;

public record SignupRequest(
		String email,
		String nickname,
		String password
) {
}
