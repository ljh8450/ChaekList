package com.example.chaeklist.domain.auth.controller;

import java.util.Map;

import com.example.chaeklist.domain.auth.dto.AuthResponse;
import com.example.chaeklist.domain.auth.dto.AuthUserResponse;
import com.example.chaeklist.domain.auth.dto.LoginRequest;
import com.example.chaeklist.domain.auth.dto.SignupRequest;
import com.example.chaeklist.domain.auth.service.AuthService;
import com.example.chaeklist.domain.auth.util.TokenService;
import com.example.chaeklist.global.auth.BearerTokenResolver;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "회원가입, 로그인, 토큰 발급 API")
public class AuthController {

	private final AuthService authService;
	private final BearerTokenResolver bearerTokenResolver;
	private final TokenService tokenService;

	public AuthController(AuthService authService, BearerTokenResolver bearerTokenResolver, TokenService tokenService) {
		this.authService = authService;
		this.bearerTokenResolver = bearerTokenResolver;
		this.tokenService = tokenService;
	}

	@PostMapping("/api/auth/signup")
	@Operation(summary = "회원가입", description = "이메일, 닉네임, 비밀번호로 새 사용자를 생성하고 accessToken/refreshToken을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "회원가입 성공")
	@ApiResponse(responseCode = "400", description = "잘못된 입력, 중복 이메일 또는 중복 닉네임")
	public AuthResponse signup(@RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/api/auth/login")
	@Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 accessToken/refreshToken을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "로그인 성공")
	@ApiResponse(responseCode = "400", description = "이메일 또는 비밀번호 불일치")
	public AuthResponse login(@RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/api/auth/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 인증 정보 조회", description = "저장된 accessToken이 유효한지 검증하고 현재 사용자 정보를 반환합니다.")
	@ApiResponse(responseCode = "200", description = "인증 사용자 조회 성공")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public AuthUserResponse me(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		return AuthUserResponse.from(tokenService.validateAccessToken(token));
	}

	@ExceptionHandler(AuthService.AuthException.class)
	public ResponseEntity<Map<String, String>> handleAuthException(AuthService.AuthException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(TokenService.TokenException.class)
	public ResponseEntity<Map<String, String>> handleInvalidToken(TokenService.TokenException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	static class UnauthorizedException extends RuntimeException {

		UnauthorizedException(String message) {
			super(message);
		}
	}
}
