package com.example.chaeklist.domain.auth.dto;

public record LoginRequest(
		String email,
		String password
) {
}
