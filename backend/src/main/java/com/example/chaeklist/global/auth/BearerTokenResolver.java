package com.example.chaeklist.global.auth;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class BearerTokenResolver {

	private static final String BEARER_PREFIX = "Bearer ";

	public Optional<String> resolve(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return Optional.empty();
		}

		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		return token.isBlank() ? Optional.empty() : Optional.of(token);
	}
}
