package com.example.chaeklist.domain.search.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.chaeklist.domain.mypage.service.MyPageService;
import com.example.chaeklist.domain.social.dto.SocialDtos.SearchItem;
import com.example.chaeklist.domain.social.dto.SocialDtos.SearchResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.SearchSection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

	private static final int DEFAULT_LIMIT = 5;
	private static final int MAX_LIMIT = 20;
	private static final Set<String> SEARCH_TYPES = Set.of("ALL", "BOOKS", "KEYWORDS", "POSTS", "USERS");

	private final JdbcTemplate jdbcTemplate;
	private final MyPageService myPageService;

	public SearchService(JdbcTemplate jdbcTemplate, MyPageService myPageService) {
		this.jdbcTemplate = jdbcTemplate;
		this.myPageService = myPageService;
	}

	public SearchResponse search(String query, String type, int limit) {
		String normalizedQuery = normalizeQuery(query);
		String normalizedType = normalizeType(type);
		int normalizedLimit = normalizeLimit(limit);

		List<SearchSection> sections = switch (normalizedType) {
			case "BOOKS" -> List.of(new SearchSection("books", searchBooks(normalizedQuery, normalizedLimit)));
			case "KEYWORDS" -> List.of(new SearchSection("keywords", searchKeywords(normalizedQuery, normalizedLimit)));
			case "POSTS" -> List.of(new SearchSection("posts", searchPosts(normalizedQuery, normalizedLimit)));
			case "USERS" -> List.of(new SearchSection("users", searchUsers(normalizedQuery, normalizedLimit)));
			default -> List.of(
					new SearchSection("books", searchBooks(normalizedQuery, normalizedLimit)),
					new SearchSection("keywords", searchKeywords(normalizedQuery, normalizedLimit)),
					new SearchSection("posts", searchPosts(normalizedQuery, normalizedLimit)),
					new SearchSection("users", searchUsers(normalizedQuery, normalizedLimit))
			);
		};

		return new SearchResponse(normalizedQuery, normalizedType.toLowerCase(Locale.ROOT), sections);
	}

	public List<SearchItem> searchBooks(String query, int limit) {
		String normalizedQuery = normalizeQuery(query);
		return jdbcTemplate.query("""
				SELECT
					b.id,
					b.title,
					b.author,
					COALESCE(primary_category.category_name, '미분류') AS category_name
				FROM books b
				LEFT JOIN (
					SELECT book_id, category_name
					FROM (
						SELECT
							bc.book_id,
							c.name AS category_name,
							ROW_NUMBER() OVER (
								PARTITION BY bc.book_id
								ORDER BY c.display_order ASC, c.name ASC, c.id ASC
							) AS rn
						FROM book_categories bc
						JOIN categories c ON c.id = bc.category_id
						WHERE c.is_active = TRUE
					) ranked_categories
					WHERE rn = 1
				) primary_category ON primary_category.book_id = b.id
				WHERE b.is_general_eligible = TRUE
					AND (
						LOWER(b.title) LIKE LOWER(?)
						OR LOWER(b.author) LIKE LOWER(?)
						OR EXISTS (
							SELECT 1
							FROM book_keywords bk
							JOIN keywords k ON k.id = bk.keyword_id
							WHERE bk.book_id = b.id
								AND LOWER(k.name) LIKE LOWER(?)
						)
					)
				ORDER BY b.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new SearchItem(
						resultSet.getString("id"),
						"book",
						resultSet.getString("title"),
						resultSet.getString("author") + " · " + resultSet.getString("category_name"),
						"/books/" + resultSet.getString("id")
				),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				normalizeLimit(limit)
		);
	}

	public List<SearchItem> searchKeywords(String query, int limit) {
		String normalizedQuery = normalizeQuery(query);
		return jdbcTemplate.query("""
				SELECT id, name, keyword_type
				FROM keywords
				WHERE LOWER(name) LIKE LOWER(?)
					AND keyword_type <> 'EXCLUDE'
				ORDER BY
					CASE keyword_type WHEN 'TREND' THEN 0 ELSE 1 END,
					name ASC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new SearchItem(
						resultSet.getString("id"),
						"keyword",
						resultSet.getString("name"),
						resultSet.getString("keyword_type"),
						"/search?query=" + resultSet.getString("name") + "&type=books"
				),
				likeQuery(normalizedQuery),
				normalizeLimit(limit)
		);
	}

	public List<SearchItem> searchPosts(String query, int limit) {
		String normalizedQuery = normalizeQuery(query);
		return jdbcTemplate.query("""
				SELECT
					sp.id,
					sp.post_type,
					CASE
						WHEN sp.author_anonymized = TRUE THEN COALESCE(sp.author_snapshot_nickname, '탈퇴한 사용자')
						ELSE COALESCE(u.nickname, sp.author_snapshot_nickname, '탈퇴한 사용자')
					END AS display_nickname,
					b.title AS book_title,
					LEFT(COALESCE(sp.content, ''), 120) AS content_summary
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				WHERE sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND (sp.user_id IS NULL OR u.status = 'ACTIVE')
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
					AND (
						LOWER(COALESCE(sp.content, '')) LIKE LOWER(?)
						OR LOWER(COALESCE(b.title, '')) LIKE LOWER(?)
						OR LOWER(COALESCE(b.author, '')) LIKE LOWER(?)
						OR LOWER(sp.post_type) LIKE LOWER(?)
					)
				ORDER BY sp.created_at DESC, sp.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new SearchItem(
						resultSet.getString("id"),
						"post",
						resultSet.getString("display_nickname") + " · " + resultSet.getString("post_type"),
						postSummary(resultSet.getString("book_title"), resultSet.getString("content_summary")),
						"/social/posts/" + resultSet.getString("id")
				),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				normalizeLimit(limit)
		);
	}

	public List<SearchItem> searchUsers(String query, int limit) {
		String normalizedQuery = normalizeQuery(query);
		return jdbcTemplate.query("""
				SELECT
					u.id,
					u.nickname,
					COUNT(DISTINCT sp.id) AS public_post_count,
					MAX(sp.created_at) AS latest_public_activity_at
				FROM users u
				LEFT JOIN user_public_profiles upp
					ON upp.user_id = u.id
				LEFT JOIN user_privacy_settings ups
					ON ups.user_id = u.id
				LEFT JOIN social_posts sp
					ON sp.user_id = u.id
					AND sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
				WHERE u.status = 'ACTIVE'
					AND (
						COALESCE(upp.profile_public, FALSE) = TRUE
						OR EXISTS (
							SELECT 1
							FROM social_posts visible_post
							WHERE visible_post.user_id = u.id
								AND visible_post.visibility = 'PUBLIC'
								AND visible_post.status = 'ACTIVE'
								AND NOT EXISTS (
									SELECT 1
									FROM social_admin_hidden_posts hidden_visible_post
									WHERE hidden_visible_post.post_id = visible_post.id
							)
						)
					)
					AND (
						LOWER(u.nickname) LIKE LOWER(?)
						OR EXISTS (
							SELECT 1
							FROM social_posts matched_post
							LEFT JOIN books matched_book ON matched_book.id = matched_post.book_id
							WHERE matched_post.user_id = u.id
								AND matched_post.visibility = 'PUBLIC'
								AND matched_post.status = 'ACTIVE'
								AND NOT EXISTS (
									SELECT 1
									FROM social_admin_hidden_posts hidden_matched_post
									WHERE hidden_matched_post.post_id = matched_post.id
								)
								AND (
									LOWER(COALESCE(matched_post.content, '')) LIKE LOWER(?)
									OR LOWER(COALESCE(matched_book.title, '')) LIKE LOWER(?)
									OR LOWER(COALESCE(matched_book.author, '')) LIKE LOWER(?)
									OR LOWER(matched_post.post_type) LIKE LOWER(?)
								)
						)
						OR (
							COALESCE(ups.interest_categories_visibility, 'PRIVATE') IN ('PUBLIC', 'PARTIAL')
							AND EXISTS (
								SELECT 1
								FROM user_interest_categories uic
								JOIN categories c ON c.id = uic.category_id
								WHERE uic.user_id = u.id
									AND c.is_active = TRUE
									AND LOWER(c.name) LIKE LOWER(?)
							)
						)
					)
				GROUP BY u.id, u.nickname
				ORDER BY
					CASE
						WHEN LOWER(u.nickname) = LOWER(?) THEN 0
						WHEN LOWER(u.nickname) LIKE LOWER(?) THEN 1
						ELSE 2
					END,
					latest_public_activity_at DESC,
					public_post_count DESC,
					u.nickname ASC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> {
					long userId = resultSet.getLong("id");
					return new SearchItem(
							resultSet.getString("id"),
							"user",
							resultSet.getString("nickname"),
							publicUserSummary(
									resultSet.getInt("public_post_count"),
									findPublicInterestCategoryNames(userId),
									resultSet.getTimestamp("latest_public_activity_at")
							),
							"/users/" + resultSet.getString("id"),
							myPageService.getPublicPrimaryReadingGrowthBadge(userId)
					);
				},
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				likeQuery(normalizedQuery),
				normalizedQuery,
				normalizedQuery + "%",
				normalizeLimit(limit)
		);
	}

	private String normalizeQuery(String query) {
		if (query == null || query.trim().length() < 2) {
			throw new SearchRequestException("Search query must be at least 2 characters.");
		}
		return query.trim();
	}

	private String normalizeType(String type) {
		if (type == null || type.isBlank()) {
			return "ALL";
		}
		String normalized = type.trim().toUpperCase(Locale.ROOT);
		if (!SEARCH_TYPES.contains(normalized)) {
			throw new SearchRequestException("Unsupported search type.");
		}
		return normalized;
	}

	private int normalizeLimit(int limit) {
		if (limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private String likeQuery(String query) {
		return "%" + query + "%";
	}

	private String postSummary(String bookTitle, String content) {
		if (bookTitle != null && !bookTitle.isBlank()) {
			return bookTitle;
		}
		if (content != null && !content.isBlank()) {
			return content;
		}
		return "공개 게시글";
	}

	private List<String> findPublicInterestCategoryNames(long userId) {
		return jdbcTemplate.query("""
				SELECT c.name
				FROM user_privacy_settings ups
				JOIN user_interest_categories uic ON uic.user_id = ups.user_id
				JOIN categories c ON c.id = uic.category_id
				WHERE ups.user_id = ?
					AND ups.interest_categories_visibility IN ('PUBLIC', 'PARTIAL')
					AND c.is_active = TRUE
				ORDER BY c.display_order ASC, c.name ASC, c.id ASC
				LIMIT 3
				""",
				(resultSet, rowNumber) -> resultSet.getString("name"),
				userId
		);
	}

	private String publicUserSummary(int publicPostCount, List<String> interestCategoryNames, Timestamp latestActivityAt) {
		List<String> parts = new ArrayList<>();
		parts.add("공개 게시글 " + publicPostCount + "개");
		if (!interestCategoryNames.isEmpty()) {
			parts.add("관심 분야 " + String.join(", ", interestCategoryNames));
		}
		if (latestActivityAt != null) {
			LocalDate latestActivityDate = latestActivityAt.toLocalDateTime().toLocalDate();
			parts.add("최근 활동 " + latestActivityDate);
		}
		return String.join(" · ", parts);
	}

	public static class SearchRequestException extends RuntimeException {

		public SearchRequestException(String message) {
			super(message);
		}
	}
}
