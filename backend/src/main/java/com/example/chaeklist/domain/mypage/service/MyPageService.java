package com.example.chaeklist.domain.mypage.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.example.chaeklist.domain.auth.dto.AuthUserResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageBookResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageInterestResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageRecommendationResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageResponse;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MyPageService {

	private static final int DEFAULT_LIMIT = 20;

	private final JdbcTemplate jdbcTemplate;

	public MyPageService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public MyPageResponse getMyPage(AuthenticatedUser user) {
		return new MyPageResponse(
				AuthUserResponse.from(user),
				getInterests(user.id()),
				getBooksByInteraction(user.id(), "READ", DEFAULT_LIMIT),
				getSavedBooks(user.id(), DEFAULT_LIMIT),
				getRecommendationHistory(user.id(), DEFAULT_LIMIT)
		);
	}

	private List<MyPageInterestResponse> getInterests(long userId) {
		return jdbcTemplate.query("""
				SELECT
					c.name AS category_name,
					COUNT(DISTINCT ubi.id) AS interaction_count
				FROM user_interest_categories uic
				JOIN categories c ON c.id = uic.category_id
				LEFT JOIN book_categories bc ON bc.category_id = c.id
				LEFT JOIN user_book_interactions ubi
					ON ubi.book_id = bc.book_id
					AND ubi.user_id = uic.user_id
					AND ubi.interaction_type IN ('VIEW', 'CLICK', 'SAVE', 'READ')
				WHERE uic.user_id = ?
					AND c.is_active = TRUE
				GROUP BY c.id, c.name, c.display_order
				ORDER BY c.display_order ASC, c.name ASC
				""",
				(resultSet, rowNumber) -> new MyPageInterestResponse(
						resultSet.getString("category_name"),
						resultSet.getString("category_name") + " 분야의 탐색과 저장 기록을 추천에 반영합니다.",
						toInterestScore(resultSet.getInt("interaction_count"))
				),
				userId
		);
	}

	private List<MyPageBookResponse> getBooksByInteraction(long userId, String interactionType, int limit) {
		return jdbcTemplate.query("""
				SELECT
					b.id,
					b.title,
					b.author,
					COALESCE(primary_category.name, '미분류') AS category_name,
					COALESCE(NULLIF(b.filter_reason, ''), '교양 필터 통과') AS tag,
					COUNT(DISTINCT view_interactions.id) AS view_count,
					COUNT(DISTINCT save_interactions.id) AS save_count,
					MAX(ubi.created_at) AS interacted_at
				FROM user_book_interactions ubi
				JOIN books b ON b.id = ubi.book_id
				LEFT JOIN book_categories bc ON bc.book_id = b.id
				LEFT JOIN categories primary_category ON primary_category.id = bc.category_id
				LEFT JOIN user_book_interactions view_interactions
					ON view_interactions.book_id = b.id
					AND view_interactions.interaction_type = 'VIEW'
				LEFT JOIN user_book_interactions save_interactions
					ON save_interactions.book_id = b.id
					AND save_interactions.interaction_type = 'SAVE'
				WHERE ubi.user_id = ?
					AND ubi.interaction_type = ?
					AND b.is_general_eligible = TRUE
				GROUP BY b.id, b.title, b.author, primary_category.name, primary_category.display_order, b.filter_reason
				ORDER BY MAX(ubi.created_at) DESC, b.id DESC
				LIMIT ?
				""",
				this::mapBook,
				userId,
				interactionType,
				limit
		);
	}

	private List<MyPageBookResponse> getSavedBooks(long userId, int limit) {
		return jdbcTemplate.query("""
				SELECT
					b.id,
					b.title,
					b.author,
					COALESCE(primary_category.name, '미분류') AS category_name,
					COALESCE(NULLIF(b.filter_reason, ''), '교양 필터 통과') AS tag,
					COUNT(DISTINCT view_interactions.id) AS view_count,
					COUNT(DISTINCT save_interactions.id) AS save_count,
					MAX(save_interactions.created_at) AS interacted_at
				FROM user_book_interactions save_interactions
				JOIN books b ON b.id = save_interactions.book_id
				LEFT JOIN user_book_interactions later_unsave
					ON later_unsave.user_id = save_interactions.user_id
					AND later_unsave.book_id = save_interactions.book_id
					AND later_unsave.interaction_type = 'UNSAVE'
					AND later_unsave.created_at > save_interactions.created_at
				LEFT JOIN book_categories bc ON bc.book_id = b.id
				LEFT JOIN categories primary_category ON primary_category.id = bc.category_id
				LEFT JOIN user_book_interactions view_interactions
					ON view_interactions.book_id = b.id
					AND view_interactions.interaction_type = 'VIEW'
				LEFT JOIN user_book_interactions all_save_interactions
					ON all_save_interactions.book_id = b.id
					AND all_save_interactions.interaction_type = 'SAVE'
				WHERE save_interactions.user_id = ?
					AND save_interactions.interaction_type = 'SAVE'
					AND later_unsave.id IS NULL
					AND b.is_general_eligible = TRUE
				GROUP BY b.id, b.title, b.author, primary_category.name, primary_category.display_order, b.filter_reason
				ORDER BY MAX(save_interactions.created_at) DESC, b.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new MyPageBookResponse(
						resultSet.getString("id"),
						resultSet.getString("title"),
						resultSet.getString("author"),
						resultSet.getString("category_name"),
						resultSet.getString("tag"),
						formatCount(resultSet.getInt("view_count")),
						resultSet.getInt("save_count"),
						resultSet.getString("category_name") + " 분야에서 저장한 교양 도서입니다.",
						readLocalDateTime(resultSet, "interacted_at")
				),
				userId,
				limit
		);
	}

	private List<MyPageRecommendationResponse> getRecommendationHistory(long userId, int limit) {
		return jdbcTemplate.query("""
				SELECT
					r.id,
					b.id AS book_id,
					b.title,
					r.recommendation_type,
					COALESCE(NULLIF(r.reason, ''), '사용자 관심 분야와 도서 행동을 바탕으로 추천했습니다.') AS reason,
					r.score,
					r.generated_at
				FROM recommendations r
				JOIN books b ON b.id = r.book_id
				WHERE r.user_id = ?
					AND b.is_general_eligible = TRUE
				ORDER BY r.generated_at DESC, r.score DESC, r.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new MyPageRecommendationResponse(
						resultSet.getLong("id"),
						resultSet.getString("book_id"),
						resultSet.getString("title"),
						resultSet.getString("reason"),
						resultSet.getString("recommendation_type"),
						toRecommendationScore(resultSet.getDouble("score")),
						readLocalDateTime(resultSet, "generated_at")
				),
				userId,
				limit
		);
	}

	private MyPageBookResponse mapBook(ResultSet resultSet, int rowNumber) throws SQLException {
		String category = resultSet.getString("category_name");
		return new MyPageBookResponse(
				resultSet.getString("id"),
				resultSet.getString("title"),
				resultSet.getString("author"),
				category,
				resultSet.getString("tag"),
				formatCount(resultSet.getInt("view_count")),
				resultSet.getInt("save_count"),
				category + " 분야의 읽은 책 기록을 취향 분석에 반영합니다.",
				readLocalDateTime(resultSet, "interacted_at")
		);
	}

	private int toInterestScore(int interactionCount) {
		if (interactionCount <= 0) {
			return 50;
		}
		return Math.min(100, 60 + interactionCount * 10);
	}

	private int toRecommendationScore(double score) {
		if (score <= 1) {
			return (int) Math.round(score * 100);
		}
		return (int) Math.round(Math.min(score, 100));
	}

	private String formatCount(int count) {
		if (count >= 1000) {
			return "%.1fk".formatted(count / 1000.0);
		}
		return String.valueOf(count);
	}

	private LocalDateTime readLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
		java.sql.Timestamp timestamp = resultSet.getTimestamp(columnName);
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}
}
