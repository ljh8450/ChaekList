package com.example.chaeklist.domain.book.controller;

import java.util.List;
import java.util.Map;

import com.example.chaeklist.domain.auth.util.TokenService;
import com.example.chaeklist.domain.book.dto.BookDetailResponse;
import com.example.chaeklist.domain.book.dto.BookSummaryResponse;
import com.example.chaeklist.domain.book.dto.HomeResponse;
import com.example.chaeklist.domain.book.service.BookService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Home & Books", description = "홈, 랭킹, 급상승, 카테고리, 책 상세 API")
public class BookController {

	private final BearerTokenResolver bearerTokenResolver;
	private final BookService bookService;
	private final TokenService tokenService;

	public BookController(BearerTokenResolver bearerTokenResolver, BookService bookService, TokenService tokenService) {
		this.bearerTokenResolver = bearerTokenResolver;
		this.bookService = bookService;
		this.tokenService = tokenService;
	}

	@GetMapping("/api/home")
	@Operation(summary = "공개 홈 조회", description = "비로그인 사용자도 접근 가능한 홈 데이터입니다. 오늘의 추천, 인기 책, 급상승 책, 카테고리별 랭킹을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "공개 홈 조회 성공")
	public HomeResponse home() {
		return bookService.getPublicHome();
	}

	@GetMapping("/api/me/home")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "개인화 홈 조회", description = "로그인 사용자를 위한 홈 데이터입니다. Swagger Authorize에 accessToken을 입력한 뒤 호출합니다.")
	@ApiResponse(responseCode = "200", description = "개인화 홈 조회 성공")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public HomeResponse myHome(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		AuthenticatedUser user = tokenService.validateAccessToken(token);
		return bookService.getPersonalHome(user);
	}

	@GetMapping("/api/books/rankings")
	@Operation(summary = "전체 랭킹 조회", description = "전체 또는 특정 카테고리의 랭킹 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "랭킹 조회 성공")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 category 또는 period")
	public List<BookSummaryResponse> rankings(
			@Parameter(description = "전체 또는 카테고리명", example = "전체") @RequestParam(defaultValue = "전체") String category,
			@Parameter(description = "기간 필터: daily, weekly, monthly", example = "weekly") @RequestParam(defaultValue = "weekly") String period,
			@Parameter(description = "반환 개수. 최대 50", example = "20") @RequestParam(defaultValue = "20") int limit
	) {
		return bookService.getRankings(category, period, limit);
	}

	@GetMapping("/api/books/trending")
	@Operation(summary = "급상승 책 조회", description = "최근 상승률 기준으로 급상승 책 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "급상승 책 조회 성공")
	public List<BookSummaryResponse> trending(
			@Parameter(description = "반환 개수. 최대 50", example = "10") @RequestParam(defaultValue = "10") int limit
	) {
		return bookService.getTrending(limit);
	}

	@GetMapping("/api/books/categories")
	@Operation(summary = "카테고리 목록 조회", description = "랭킹과 홈에서 사용할 수 있는 카테고리 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "카테고리 조회 성공")
	public List<String> categories() {
		return bookService.getCategories();
	}

	@GetMapping("/api/books/categories/{category}/rankings")
	@Operation(summary = "카테고리별 랭킹 조회", description = "특정 카테고리의 랭킹 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "카테고리 랭킹 조회 성공")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 category 또는 period")
	public List<BookSummaryResponse> categoryRankings(
			@Parameter(description = "카테고리명", example = "경제") @PathVariable String category,
			@Parameter(description = "기간 필터: daily, weekly, monthly", example = "weekly") @RequestParam(defaultValue = "weekly") String period,
			@Parameter(description = "반환 개수. 최대 50", example = "20") @RequestParam(defaultValue = "20") int limit
	) {
		return bookService.getCategoryRankings(category, period, limit);
	}

	@GetMapping("/api/books/{bookId}")
	@Operation(summary = "책 상세 조회", description = "책 상세 정보와 비슷한 책 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "책 상세 조회 성공")
	@ApiResponse(responseCode = "404", description = "존재하지 않는 책")
	public BookDetailResponse bookDetail(
			@Parameter(description = "책 ID", example = "1") @PathVariable String bookId
	) {
		return bookService.getBookDetail(bookId);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(TokenService.TokenException.class)
	public ResponseEntity<Map<String, String>> handleInvalidToken(TokenService.TokenException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BookService.BookNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(BookService.BookNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BookService.BookRequestException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(BookService.BookRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	static class UnauthorizedException extends RuntimeException {

		UnauthorizedException(String message) {
			super(message);
		}
	}
}
