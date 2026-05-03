package com.example.chaeklist.domain.book.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse;
import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse.ItemResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class BookImageEnrichmentService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;

	private final String kakaoApiKey;
	private final String kakaoBookSearchUrl;
	private final JdbcTemplate jdbcTemplate;
	private final RestClient restClient;

	public BookImageEnrichmentService(
			@Value("${KAKAO_REST_API_KEY:}") String kakaoApiKey,
			@Value("${KAKAO_BOOK_SEARCH_URL:https://dapi.kakao.com/v3/search/book}") String kakaoBookSearchUrl,
			JdbcTemplate jdbcTemplate,
			RestClient.Builder restClientBuilder
	) {
		this.kakaoApiKey = kakaoApiKey;
		this.kakaoBookSearchUrl = kakaoBookSearchUrl;
		this.jdbcTemplate = jdbcTemplate;
		this.restClient = restClientBuilder.build();
	}

	public BookImageEnrichmentResponse enrichMissingCoverImages(int requestedLimit) {
		if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
			throw new BookImageEnrichmentException("KAKAO_REST_API_KEY is required.");
		}

		List<BookImageTarget> targets = findTargets(normalizeLimit(requestedLimit));
		int updated = 0;
		int skipped = 0;
		int failed = 0;
		java.util.ArrayList<ItemResult> items = new java.util.ArrayList<>();

		for (BookImageTarget target : targets) {
			try {
				SearchResult searchResult = searchCoverImageUrl(target);
				if (searchResult.imageUrl().isEmpty()) {
					skipped++;
					items.add(toItemResult(target, "SKIPPED", searchResult.reason(), searchResult.queries(), null));
					continue;
				}
				int updateCount = updateCoverImageUrl(target.id(), searchResult.imageUrl().get());
				updated += updateCount;
				items.add(toItemResult(target, updateCount > 0 ? "UPDATED" : "SKIPPED",
						updateCount > 0 ? "UPDATED_WITH_KAKAO_THUMBNAIL" : "COVER_URL_NOT_UPDATABLE",
						searchResult.queries(), searchResult.imageUrl().get()));
			} catch (RestClientException exception) {
				failed++;
				items.add(toItemResult(target, "FAILED", exception.getClass().getSimpleName(), List.of(searchQuery(target)), null));
			}
		}

		return new BookImageEnrichmentResponse(targets.size(), updated, skipped, failed, items);
	}

	private List<BookImageTarget> findTargets(int limit) {
		return jdbcTemplate.query("""
				SELECT id, title, author, isbn13
				FROM books
				WHERE is_general_eligible = TRUE
					AND (cover_image_url IS NULL OR cover_image_url = '' OR cover_image_url LIKE '/book-covers/%')
				ORDER BY id ASC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new BookImageTarget(
						resultSet.getLong("id"),
						resultSet.getString("title"),
						resultSet.getString("author"),
						resultSet.getString("isbn13")
				),
				limit
		);
	}

	private SearchResult searchCoverImageUrl(BookImageTarget target) {
		java.util.ArrayList<String> queries = new java.util.ArrayList<>();
		for (String query : searchQueries(target)) {
			if (query.isBlank() || queries.contains(query)) {
				continue;
			}
			queries.add(query);
			Optional<String> imageUrl = searchCoverImageUrl(query, target);
			if (imageUrl.isPresent()) {
				return new SearchResult(imageUrl, queries, "FOUND_KAKAO_THUMBNAIL");
			}
		}
		return new SearchResult(Optional.empty(), queries, "NO_KAKAO_THUMBNAIL");
	}

	private Optional<String> searchCoverImageUrl(String query, BookImageTarget target) {
		KakaoBookSearchResponse response = restClient.get()
				.uri(kakaoBookSearchUrl, uriBuilder -> uriBuilder
						.queryParam("query", query)
						.queryParam("size", 10)
						.build())
				.header("Authorization", "KakaoAK " + kakaoApiKey)
				.retrieve()
				.body(KakaoBookSearchResponse.class);

		if (response == null || response.documents() == null) {
			return Optional.empty();
		}

		return response.documents().stream()
				.filter(document -> document.thumbnail() != null && !document.thumbnail().isBlank())
				.sorted((first, second) -> Integer.compare(matchScore(second, target), matchScore(first, target)))
				.map(KakaoBookDocument::thumbnail)
				.findFirst();
	}

	private List<String> searchQueries(BookImageTarget target) {
		if (target.isbn13() != null && !target.isbn13().isBlank()) {
			return List.of(target.isbn13(), titleAuthorQuery(target), target.title());
		}
		return List.of(titleAuthorQuery(target), target.title());
	}

	private String searchQuery(BookImageTarget target) {
		if (target.isbn13() != null && !target.isbn13().isBlank()) {
			return target.isbn13();
		}
		return titleAuthorQuery(target);
	}

	private String titleAuthorQuery(BookImageTarget target) {
		return target.title() + " " + target.author();
	}

	private int updateCoverImageUrl(long bookId, String imageUrl) {
		return jdbcTemplate.update("""
				UPDATE books
				SET cover_image_url = ?,
					source_provider = COALESCE(NULLIF(source_provider, ''), 'KAKAO'),
					updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
					AND (cover_image_url IS NULL OR cover_image_url = '' OR cover_image_url LIKE '/book-covers/%')
				""", imageUrl, bookId);
	}

	private int matchScore(KakaoBookDocument document, BookImageTarget target) {
		int score = 0;
		String documentTitle = normalize(document.title());
		String targetTitle = normalize(target.title());
		if (!targetTitle.isBlank() && documentTitle.contains(targetTitle)) {
			score += 60;
		}
		if (!targetTitle.isBlank() && targetTitle.contains(documentTitle)) {
			score += 30;
		}
		if (document.authors() != null && document.authors().stream()
				.filter(Objects::nonNull)
				.map(this::normalize)
				.anyMatch(author -> !author.isBlank() && normalize(target.author()).contains(author))) {
			score += 40;
		}
		return score;
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}]", "");
	}

	private int normalizeLimit(int limit) {
		if (limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private ItemResult toItemResult(
			BookImageTarget target,
			String status,
			String reason,
			List<String> queries,
			String imageUrl
	) {
		return new ItemResult(String.valueOf(target.id()), target.title(), target.author(), status, reason, queries, imageUrl);
	}

	public static class BookImageEnrichmentException extends RuntimeException {

		public BookImageEnrichmentException(String message) {
			super(message);
		}
	}

	private record BookImageTarget(long id, String title, String author, String isbn13) {
	}

	private record SearchResult(Optional<String> imageUrl, List<String> queries, String reason) {
	}

	private record KakaoBookSearchResponse(List<KakaoBookDocument> documents) {
	}

	private record KakaoBookDocument(String title, List<String> authors, String thumbnail) {
	}
}
