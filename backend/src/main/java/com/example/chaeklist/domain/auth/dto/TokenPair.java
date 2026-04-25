package com.example.chaeklist.domain.auth.dto;

public record TokenPair(
		String accessToken,
		String refreshToken
) {
}
