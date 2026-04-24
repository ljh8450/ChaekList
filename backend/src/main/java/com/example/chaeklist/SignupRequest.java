package com.example.chaeklist;

public record SignupRequest(
		String email,
		String nickname,
		String password
) {
}
