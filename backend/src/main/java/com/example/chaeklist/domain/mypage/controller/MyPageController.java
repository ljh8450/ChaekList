package com.example.chaeklist.domain.mypage.controller;

import java.util.Map;

import com.example.chaeklist.domain.auth.util.TokenService;
import com.example.chaeklist.domain.mypage.dto.MyPageResponse;
import com.example.chaeklist.domain.mypage.service.MyPageService;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import com.example.chaeklist.global.auth.BearerTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "My Page", description = "마이페이지 API")
public class MyPageController {

	private final BearerTokenResolver bearerTokenResolver;
	private final MyPageService myPageService;
	private final TokenService tokenService;

	public MyPageController(BearerTokenResolver bearerTokenResolver, MyPageService myPageService, TokenService tokenService) {
		this.bearerTokenResolver = bearerTokenResolver;
		this.myPageService = myPageService;
		this.tokenService = tokenService;
	}

	@GetMapping("/api/me/mypage")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "마이페이지 조회", description = "관심 분야, 읽은 책, 저장한 책, 추천 히스토리를 실제 DB 데이터로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "마이페이지 조회 성공")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public MyPageResponse myPage(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		AuthenticatedUser user = tokenService.validateAccessToken(token);
		return myPageService.getMyPage(user);
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
