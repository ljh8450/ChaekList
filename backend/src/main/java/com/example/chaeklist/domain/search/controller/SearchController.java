package com.example.chaeklist.domain.search.controller;

import java.util.List;
import java.util.Map;

import com.example.chaeklist.domain.search.service.SearchService;
import com.example.chaeklist.domain.social.dto.SocialDtos.SearchItem;
import com.example.chaeklist.domain.social.dto.SocialDtos.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Search", description = "Header 통합 검색 API")
public class SearchController {

	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping("/api/search")
	@Operation(summary = "통합 검색", description = "책, 키워드, 공개 게시물, 공개 유저를 통합 검색합니다.")
	@ApiResponse(responseCode = "200", description = "검색 성공")
	@ApiResponse(responseCode = "400", description = "검색어가 짧거나 지원하지 않는 type")
	public SearchResponse search(
			@Parameter(description = "검색어. 최소 2자", example = "투자") @RequestParam String query,
			@Parameter(description = "검색 유형", example = "all") @RequestParam(defaultValue = "all") String type,
			@Parameter(description = "섹션별 반환 개수. 최대 20", example = "5") @RequestParam(defaultValue = "5") int limit
	) {
		return searchService.search(query, type, limit);
	}

	@GetMapping("/api/search/books")
	@Operation(summary = "책 검색", description = "제목, 저자, 키워드를 대상으로 책을 검색합니다.")
	public List<SearchItem> searchBooks(
			@RequestParam String query,
			@RequestParam(defaultValue = "10") int limit
	) {
		return searchService.searchBooks(query, limit);
	}

	@GetMapping("/api/search/posts")
	@Operation(summary = "공개 게시물 검색", description = "PUBLIC/ACTIVE 게시글만 검색합니다.")
	public List<SearchItem> searchPosts(
			@RequestParam String query,
			@RequestParam(defaultValue = "10") int limit
	) {
		return searchService.searchPosts(query, limit);
	}

	@GetMapping("/api/search/users")
	@Operation(summary = "공개 유저 검색", description = "공개 프로필이 허용된 활성 사용자 nickname만 검색합니다.")
	public List<SearchItem> searchUsers(
			@RequestParam String query,
			@RequestParam(defaultValue = "10") int limit
	) {
		return searchService.searchUsers(query, limit);
	}

	@ExceptionHandler(SearchService.SearchRequestException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(SearchService.SearchRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}
}
