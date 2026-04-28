package com.example.chaeklist.domain.book.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse;
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

		for (BookImageTarget target : targets) {
			try {
				Optional<String> imageUrl = searchCoverImageUrl(target);
				if (imageUrl.isEmpty()) {
					skipped++;
					continue;
				}
				updated += updateCoverImageUrl(target.id(), imageUrl.get());
			} catch (RestClientException exception) {
				failed++;
			}
		}

		return new BookImageEnrichmentResponse(targets.size(), updated, skipped, failed);
	}

	private List<BookImageTarget> findTargets(int limit) {
		return jdbcTemplate.query("""
				SELECT id, title, author, isbn13
				FROM books
				WHERE is_general_eligible = TRUE
					AND (cover_image_url IS NULL OR cover_image_url = '')
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

	private Optional<String> searchCoverImageUrl(BookImageTarget target) {
		KakaoBookSearchResponse response = restClient.get()
				.uri(kakaoBookSearchUrl, uriBuilder -> uriBuilder
						.queryParam("query", searchQuery(target))
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

	private String searchQuery(BookImageTarget target) {
		if (target.isbn13() != null && !target.isbn13().isBlank()) {
			return target.isbn13();
		}
		return target.title() + " " + target.author();
	}

	private int updateCoverImageUrl(long bookId, String imageUrl) {
		return jdbcTemplate.update("""
				UPDATE books
				SET cover_image_url = ?,
					source_provider = COALESCE(NULLIF(source_provider, ''), 'KAKAO'),
					updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
					AND (cover_image_url IS NULL OR cover_image_url = '')
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

	public static class BookImageEnrichmentException extends RuntimeException {

		public BookImageEnrichmentException(String message) {
			super(message);
		}
	}

	private record BookImageTarget(long id, String title, String author, String isbn13) {
	}

	private record KakaoBookSearchResponse(List<KakaoBookDocument> documents) {
	}

	private record KakaoBookDocument(String title, List<String> authors, String thumbnail) {
	}
}
