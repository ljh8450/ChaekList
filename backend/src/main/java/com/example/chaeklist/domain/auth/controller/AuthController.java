package com.example.chaeklist.domain.auth.controller;

import java.util.Map;

import com.example.chaeklist.domain.auth.dto.AuthResponse;
import com.example.chaeklist.domain.auth.dto.LoginRequest;
import com.example.chaeklist.domain.auth.dto.SignupRequest;
import com.example.chaeklist.domain.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/api/auth/signup")
	public AuthResponse signup(@RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/api/auth/login")
	public AuthResponse login(@RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@ExceptionHandler(AuthService.AuthException.class)
	public ResponseEntity<Map<String, String>> handleAuthException(AuthService.AuthException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}
}
