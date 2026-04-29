package com.example.chaeklist.domain.mypage.controller;

import java.util.Map;

import com.example.chaeklist.domain.auth.util.TokenService;
import com.example.chaeklist.domain.mypage.dto.BookInteractionRequest;
import com.example.chaeklist.domain.mypage.dto.BookInteractionResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageResponse;
import com.example.chaeklist.domain.mypage.dto.OnboardingOptionsResponse;
import com.example.chaeklist.domain.mypage.dto.OnboardingRequest;
import com.example.chaeklist.domain.mypage.dto.OnboardingStatusResponse;
import com.example.chaeklist.domain.mypage.dto.ReadingGrowthResponse.Badge;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
		AuthenticatedUser user = authenticate(authorizationHeader);
		return myPageService.getMyPage(user);
	}

	@GetMapping("/api/me/reading-growth/primary-badge")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "대표 독서 성장 배지 조회", description = "헤더 표시용 대표 독서 성장 배지만 가볍게 조회합니다.")
	@ApiResponse(responseCode = "200", description = "대표 배지 조회 성공")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public Badge primaryReadingGrowthBadge(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		AuthenticatedUser user = authenticate(authorizationHeader);
		return myPageService.getPrimaryReadingGrowthBadge(user);
	}

	@GetMapping("/api/me/onboarding-status")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "온보딩 완료 여부 조회", description = "users.onboarding_completed 값을 기준으로 현재 사용자의 온보딩 완료 여부를 반환합니다.")
	@ApiResponse(responseCode = "200", description = "온보딩 상태 조회 성공")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public OnboardingStatusResponse onboardingStatus(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		AuthenticatedUser user = authenticate(authorizationHeader);
		return myPageService.getOnboardingStatus(user);
	}

	@GetMapping("/api/me/onboarding-options")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "온보딩 선택지 조회", description = "온보딩 화면에서 선택할 활성 카테고리와 교양 도서 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "온보딩 선택지 조회 성공")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public OnboardingOptionsResponse onboardingOptions(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		authenticate(authorizationHeader);
		return myPageService.getOnboardingOptions();
	}

	@PutMapping("/api/me/onboarding")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "온보딩 저장", description = "관심 분야를 교체 저장하고 읽은 책을 READ interaction으로 기록합니다.")
	@ApiResponse(responseCode = "200", description = "온보딩 저장 성공")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 category/book ID 또는 빈 요청")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public OnboardingStatusResponse saveOnboarding(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody OnboardingRequest request
	) {
		AuthenticatedUser user = authenticate(authorizationHeader);
		myPageService.saveOnboarding(user, request);
		return new OnboardingStatusResponse(true);
	}

	@PostMapping("/api/me/books/{bookId}/interactions")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "책 행동 기록", description = "현재 사용자의 책 저장, 저장 취소, 읽음 행동을 기록하고 최신 상태를 반환합니다.")
	@ApiResponse(responseCode = "200", description = "책 행동 기록 성공")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 interaction type")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	@ApiResponse(responseCode = "404", description = "존재하지 않거나 교양 대상이 아닌 책")
	public BookInteractionResponse saveBookInteraction(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@Parameter(description = "책 ID", example = "301") @PathVariable String bookId,
			@RequestBody BookInteractionRequest request
	) {
		AuthenticatedUser user = authenticate(authorizationHeader);
		return myPageService.saveBookInteraction(user, bookId, request);
	}

	private AuthenticatedUser authenticate(String authorizationHeader) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		return tokenService.validateAccessToken(token);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(TokenService.TokenException.class)
	public ResponseEntity<Map<String, String>> handleInvalidToken(TokenService.TokenException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(MyPageService.OnboardingRequestException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(MyPageService.OnboardingRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(MyPageService.BookInteractionRequestException.class)
	public ResponseEntity<Map<String, String>> handleInteractionBadRequest(MyPageService.BookInteractionRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(MyPageService.BookInteractionBookNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleInteractionNotFound(MyPageService.BookInteractionBookNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	static class UnauthorizedException extends RuntimeException {

		UnauthorizedException(String message) {
			super(message);
		}
	}
}
