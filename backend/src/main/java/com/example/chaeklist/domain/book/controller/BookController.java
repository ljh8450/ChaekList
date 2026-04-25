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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
	public HomeResponse home() {
		return bookService.getPublicHome();
	}

	@GetMapping("/api/me/home")
	@SecurityRequirement(name = "bearerAuth")
	public HomeResponse myHome(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		AuthenticatedUser user = tokenService.validateAccessToken(token);
		return bookService.getPersonalHome(user);
	}

	@GetMapping("/api/books/rankings")
	public List<BookSummaryResponse> rankings(
			@RequestParam(defaultValue = "전체") String category,
			@RequestParam(defaultValue = "weekly") String period,
			@RequestParam(defaultValue = "20") int limit
	) {
		return bookService.getRankings(category, period, limit);
	}

	@GetMapping("/api/books/trending")
	public List<BookSummaryResponse> trending(@RequestParam(defaultValue = "10") int limit) {
		return bookService.getTrending(limit);
	}

	@GetMapping("/api/books/categories")
	public List<String> categories() {
		return bookService.getCategories();
	}

	@GetMapping("/api/books/categories/{category}/rankings")
	public List<BookSummaryResponse> categoryRankings(
			@PathVariable String category,
			@RequestParam(defaultValue = "weekly") String period,
			@RequestParam(defaultValue = "20") int limit
	) {
		return bookService.getCategoryRankings(category, period, limit);
	}

	@GetMapping("/api/books/{bookId}")
	public BookDetailResponse bookDetail(@PathVariable String bookId) {
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
