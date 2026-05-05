package com.example.chaeklist.domain.mypage.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.chaeklist.domain.auth.dto.AuthUserResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageBookResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageInterestResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageRecommendationResponse;
import com.example.chaeklist.domain.mypage.dto.MyPageResponse;
import com.example.chaeklist.domain.mypage.dto.BookInteractionRequest;
import com.example.chaeklist.domain.mypage.dto.BookInteractionResponse;
import com.example.chaeklist.domain.mypage.dto.OnboardingBookOptionResponse;
import com.example.chaeklist.domain.mypage.dto.OnboardingCategoryOptionResponse;
import com.example.chaeklist.domain.mypage.dto.OnboardingOptionsResponse;
import com.example.chaeklist.domain.mypage.dto.OnboardingRequest;
import com.example.chaeklist.domain.mypage.dto.OnboardingStatusResponse;
import com.example.chaeklist.domain.mypage.dto.ReadingGrowthResponse;
import com.example.chaeklist.domain.mypage.dto.ReadingGrowthResponse.Badge;
import com.example.chaeklist.domain.mypage.dto.ReadingPurposeResponse;
import com.example.chaeklist.domain.mypage.model.ReadingPurpose;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyPageService {

	private static final int DEFAULT_LIMIT = 20;
	private static final Set<String> SUPPORTED_BOOK_INTERACTIONS = Set.of("SAVE", "UNSAVE", "READ", "DISMISS");

	private final JdbcTemplate jdbcTemplate;
	private final String readingGrowthToday;

	public MyPageService(JdbcTemplate jdbcTemplate, @Value("${chaeklist.reading-growth.today:}") String readingGrowthToday) {
		this.jdbcTemplate = jdbcTemplate;
		this.readingGrowthToday = readingGrowthToday;
	}

	public MyPageResponse getMyPage(AuthenticatedUser user) {
		return new MyPageResponse(
				AuthUserResponse.from(user),
				getInterests(user.id()),
				getReadingPurposes(user.id()),
				getBooksByInteraction(user.id(), "READ", DEFAULT_LIMIT),
				getSavedBooks(user.id(), DEFAULT_LIMIT),
				getRecommendationHistory(user.id(), DEFAULT_LIMIT),
				getReadingGrowth(user.id())
		);
	}

	public Badge getPrimaryReadingGrowthBadge(AuthenticatedUser user) {
		return getPrimaryReadingGrowthBadge(user.id());
	}

	public Badge getPrimaryReadingGrowthBadge(long userId) {
		return selectPrimaryBadge(getReadingGrowthBadges(getReadingGrowthBadgeMetrics(userId)));
	}

	public Badge getPublicPrimaryReadingGrowthBadge(long userId) {
		if (!isBadgesPublic(userId)) {
			return null;
		}
		return getPrimaryReadingGrowthBadge(userId);
	}

	public OnboardingStatusResponse getOnboardingStatus(AuthenticatedUser user) {
		Boolean completed = jdbcTemplate.queryForObject(
				"SELECT onboarding_completed FROM users WHERE id = ?",
				Boolean.class,
				user.id()
		);
		return new OnboardingStatusResponse(Boolean.TRUE.equals(completed));
	}

	public OnboardingOptionsResponse getOnboardingOptions() {
		return new OnboardingOptionsResponse(
				getOnboardingCategories(),
				getOnboardingBooks(DEFAULT_LIMIT),
				getReadingPurposeOptions()
		);
	}

	@Transactional
	public void saveOnboarding(AuthenticatedUser user, OnboardingRequest request) {
		List<Long> categoryIds = normalizeIds(request == null ? null : request.categoryIds());
		List<Long> readBookIds = normalizeIds(request == null ? null : request.readBookIds());
		List<ReadingPurpose> readingPurposes = normalizeReadingPurposes(request == null ? null : request.readingPurposeCodes());

		if (categoryIds.isEmpty()) {
			throw new OnboardingRequestException("At least one category is required.");
		}
		if (readBookIds.isEmpty()) {
			throw new OnboardingRequestException("At least one read book is required.");
		}

		validateCategories(categoryIds);
		validateBooks(readBookIds);

		jdbcTemplate.update("DELETE FROM user_interest_categories WHERE user_id = ?", user.id());
		for (Long categoryId : categoryIds) {
			jdbcTemplate.update("""
					INSERT INTO user_interest_categories (user_id, category_id, created_at)
					VALUES (?, ?, CURRENT_TIMESTAMP)
					""", user.id(), categoryId);
		}

		jdbcTemplate.update("DELETE FROM user_book_interactions WHERE user_id = ? AND interaction_type = 'READ'", user.id());
		for (Long bookId : readBookIds) {
			insertReadInteraction(user.id(), bookId);
		}

		jdbcTemplate.update("DELETE FROM user_reading_purposes WHERE user_id = ?", user.id());
		for (ReadingPurpose purpose : readingPurposes) {
			jdbcTemplate.update("""
					INSERT INTO user_reading_purposes (user_id, purpose_code, created_at)
					VALUES (?, ?, CURRENT_TIMESTAMP)
					""", user.id(), purpose.code());
		}

		jdbcTemplate.update("""
				UPDATE users
				SET onboarding_completed = TRUE,
					updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", user.id());
	}

	@Transactional
	public BookInteractionResponse saveBookInteraction(AuthenticatedUser user, String bookId, BookInteractionRequest request) {
		long numericBookId = parseBookId(bookId);
		validateBook(numericBookId);

		String type = normalizeInteractionType(request == null ? null : request.type());
		if (!SUPPORTED_BOOK_INTERACTIONS.contains(type)) {
			throw new BookInteractionRequestException("Unsupported interaction type.");
		}

		if (shouldInsertInteraction(user.id(), numericBookId, type)) {
			insertInteraction(user.id(), numericBookId, type);
		}
		return getBookInteractionState(user.id(), numericBookId);
	}

	private List<OnboardingCategoryOptionResponse> getOnboardingCategories() {
		return jdbcTemplate.query("""
				SELECT id, name
				FROM categories
				WHERE is_active = TRUE
				ORDER BY display_order ASC, name ASC
				""",
				(resultSet, rowNumber) -> new OnboardingCategoryOptionResponse(
						resultSet.getLong("id"),
						resultSet.getString("name"),
						resultSet.getString("name") + " 분야의 탐색과 추천에 반영합니다."
				)
		);
	}

	private List<OnboardingBookOptionResponse> getOnboardingBooks(int limit) {
		return jdbcTemplate.query("""
				SELECT
					b.id,
					b.title,
					b.author,
					COALESCE(primary_category.name, '미분류') AS category_name,
					COALESCE(NULLIF(b.filter_reason, ''), '교양 필터 통과') AS reason
				FROM books b
				LEFT JOIN (
					SELECT book_id, category_id
					FROM (
						SELECT
							bc.book_id,
							c.id AS category_id,
							ROW_NUMBER() OVER (
								PARTITION BY bc.book_id
								ORDER BY c.display_order ASC, c.name ASC, c.id ASC
							) AS rn
						FROM book_categories bc
						JOIN categories c ON c.id = bc.category_id
						WHERE c.is_active = TRUE
					) ranked_categories
					WHERE rn = 1
				) primary_category_link ON primary_category_link.book_id = b.id
				LEFT JOIN categories primary_category ON primary_category.id = primary_category_link.category_id
				WHERE b.is_general_eligible = TRUE
				ORDER BY COALESCE(primary_category.display_order, 9999) ASC, b.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new OnboardingBookOptionResponse(
						resultSet.getString("id"),
						resultSet.getString("title"),
						resultSet.getString("author"),
						resultSet.getString("category_name"),
						resultSet.getString("category_name") + " 분야의 읽은 책 기록을 추천에 반영합니다."
				),
				limit
		);
	}

	private List<ReadingPurposeResponse> getReadingPurposeOptions() {
		return ReadingPurpose.options().stream()
				.map(ReadingPurposeResponse::from)
				.toList();
	}

	private List<MyPageInterestResponse> getInterests(long userId) {
		return jdbcTemplate.query("""
				SELECT
					c.id AS category_id,
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
						resultSet.getLong("category_id"),
						resultSet.getString("category_name"),
						resultSet.getString("category_name") + " 분야의 탐색과 저장 기록을 추천에 반영합니다.",
						toInterestScore(resultSet.getInt("interaction_count"))
				),
				userId
		);
	}

	private List<ReadingPurposeResponse> getReadingPurposes(long userId) {
		return jdbcTemplate.queryForList("""
				SELECT purpose_code
				FROM user_reading_purposes
				WHERE user_id = ?
				ORDER BY created_at ASC, purpose_code ASC
				""", String.class, userId).stream()
				.map(ReadingPurpose::fromCode)
				.flatMap(java.util.Optional::stream)
				.map(ReadingPurposeResponse::from)
				.toList();
	}

	private List<Long> normalizeIds(List<Long> ids) {
		if (ids == null) {
			return List.of();
		}
		return ids.stream()
				.filter(id -> id != null && id > 0)
				.distinct()
				.toList();
	}

	private List<ReadingPurpose> normalizeReadingPurposes(List<String> purposeCodes) {
		if (purposeCodes == null) {
			return List.of();
		}

		List<ReadingPurpose> purposes = purposeCodes.stream()
				.filter(code -> code != null && !code.isBlank())
				.map(code -> ReadingPurpose.fromCode(code)
						.orElseThrow(() -> new OnboardingRequestException("Unsupported reading purpose code.")))
				.distinct()
				.toList();

		if (purposes.size() > 3) {
			throw new OnboardingRequestException("Reading purposes must be 3 or fewer.");
		}

		return purposes;
	}

	private void validateCategories(List<Long> categoryIds) {
		Set<Long> activeCategoryIds = new HashSet<>(jdbcTemplate.queryForList("""
				SELECT id
				FROM categories
				WHERE id IN (%s)
					AND is_active = TRUE
				""".formatted(placeholders(categoryIds.size())), Long.class, categoryIds.toArray()));

		if (activeCategoryIds.size() != categoryIds.size()) {
			throw new OnboardingRequestException("Unsupported category id.");
		}
	}

	private void validateBooks(List<Long> bookIds) {
		Set<Long> eligibleBookIds = new HashSet<>(jdbcTemplate.queryForList("""
				SELECT id
				FROM books
				WHERE id IN (%s)
					AND is_general_eligible = TRUE
				""".formatted(placeholders(bookIds.size())), Long.class, bookIds.toArray()));

		if (eligibleBookIds.size() != bookIds.size()) {
			throw new OnboardingRequestException("Unsupported book id.");
		}
	}

	private String placeholders(int count) {
		return String.join(", ", java.util.Collections.nCopies(count, "?"));
	}

	private void insertReadInteraction(long userId, long bookId) {
		jdbcTemplate.update("""
				INSERT INTO user_book_interactions (user_id, book_id, interaction_type, created_at)
				VALUES (?, ?, 'READ', CURRENT_TIMESTAMP)
				""", userId, bookId);
	}

	private boolean shouldInsertInteraction(long userId, long bookId, String type) {
		return switch (type) {
			case "SAVE" -> !isSaved(userId, bookId);
			case "UNSAVE" -> isSaved(userId, bookId);
			case "READ" -> !isRead(userId, bookId);
			case "DISMISS" -> !isDismissed(userId, bookId);
			default -> false;
		};
	}

	private void insertInteraction(long userId, long bookId, String type) {
		jdbcTemplate.update("""
				INSERT INTO user_book_interactions (user_id, book_id, interaction_type, created_at)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP)
				""", userId, bookId, type);
	}

	private BookInteractionResponse getBookInteractionState(long userId, long bookId) {
		return new BookInteractionResponse(String.valueOf(bookId), isSaved(userId, bookId), isRead(userId, bookId), isDismissed(userId, bookId));
	}

	private boolean isSaved(long userId, long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions save_interactions
				LEFT JOIN user_book_interactions later_unsave
					ON later_unsave.user_id = save_interactions.user_id
					AND later_unsave.book_id = save_interactions.book_id
					AND later_unsave.interaction_type = 'UNSAVE'
					AND (
						later_unsave.created_at > save_interactions.created_at
						OR (
							later_unsave.created_at = save_interactions.created_at
							AND later_unsave.id > save_interactions.id
						)
					)
				WHERE save_interactions.user_id = ?
					AND save_interactions.book_id = ?
					AND save_interactions.interaction_type = 'SAVE'
					AND later_unsave.id IS NULL
				""", Integer.class, userId, bookId);
		return count != null && count > 0;
	}

	private boolean isRead(long userId, long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions
				WHERE user_id = ?
					AND book_id = ?
					AND interaction_type = 'READ'
				""", Integer.class, userId, bookId);
		return count != null && count > 0;
	}

	private boolean isDismissed(long userId, long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions
				WHERE user_id = ?
					AND book_id = ?
					AND interaction_type = 'DISMISS'
				""", Integer.class, userId, bookId);
		return count != null && count > 0;
	}

	private String normalizeInteractionType(String type) {
		if (type == null || type.isBlank()) {
			throw new BookInteractionRequestException("Interaction type is required.");
		}
		return type.trim().toUpperCase();
	}

	private long parseBookId(String bookId) {
		try {
			return Long.parseLong(bookId);
		} catch (NumberFormatException exception) {
			throw new BookInteractionBookNotFoundException("Book not found.");
		}
	}

	private void validateBook(long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM books
				WHERE id = ?
					AND is_general_eligible = TRUE
				""", Integer.class, bookId);
		if (count == null || count == 0) {
			throw new BookInteractionBookNotFoundException("Book not found.");
		}
	}

	private List<MyPageBookResponse> getBooksByInteraction(long userId, String interactionType, int limit) {
		return jdbcTemplate.query("""
				SELECT
					b.id,
					b.title,
					b.author,
					b.cover_image_url,
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
				GROUP BY b.id, b.title, b.author, b.cover_image_url, primary_category.name, primary_category.display_order, b.filter_reason
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
					b.cover_image_url,
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
					AND (
						later_unsave.created_at > save_interactions.created_at
						OR (
							later_unsave.created_at = save_interactions.created_at
							AND later_unsave.id > save_interactions.id
						)
					)
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
				GROUP BY b.id, b.title, b.author, b.cover_image_url, primary_category.name, primary_category.display_order, b.filter_reason
				ORDER BY MAX(save_interactions.created_at) DESC, b.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new MyPageBookResponse(
						resultSet.getString("id"),
						resultSet.getString("title"),
						resultSet.getString("author"),
						resultSet.getString("cover_image_url"),
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
					r.created_at
				FROM recommendations r
				JOIN books b ON b.id = r.book_id
				WHERE r.user_id = ?
					AND b.is_general_eligible = TRUE
				ORDER BY r.created_at DESC, r.score DESC, r.id DESC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new MyPageRecommendationResponse(
						resultSet.getLong("id"),
						resultSet.getString("book_id"),
						resultSet.getString("title"),
						resultSet.getString("reason"),
						resultSet.getString("recommendation_type"),
						toRecommendationScore(resultSet.getDouble("score")),
						readLocalDateTime(resultSet, "created_at")
				),
				userId,
				limit
		);
	}

	private ReadingGrowthResponse getReadingGrowth(long userId) {
		ReadingGrowthMetrics metrics = getReadingGrowthMetrics(userId);
		int score = metrics.totalReadCount() * 10
				+ metrics.savedToReadCount() * 12
				+ metrics.categoryDiversityCount() * 15
				+ metrics.purposeMatchReadCount() * 10
				+ metrics.recommendationSavedCount() * 5
				+ metrics.recommendationReadCount() * 15
				+ metrics.socialActivityScore()
				+ metrics.readingRoomActivityScore();

		List<Badge> badges = getReadingGrowthBadges(metrics);
		Badge primaryBadge = selectPrimaryBadge(badges);

		return new ReadingGrowthResponse(
				toReadingGrowthLevel(score),
				toReadingGrowthProgress(score),
				toReadingGrowthSummary(metrics),
				metrics.monthlyReadCount(),
				metrics.savedToReadCount(),
				metrics.categoryDiversityCount(),
				metrics.recommendationConversionCount(),
				primaryBadge,
				badges
		);
	}

	private ReadingGrowthMetrics getReadingGrowthMetrics(long userId) {
		LocalDate today = readingGrowthToday == null || readingGrowthToday.isBlank()
				? LocalDate.now()
				: LocalDate.parse(readingGrowthToday);
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime nextMonthStart = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
		int monthlyReadCount = countMonthlyReadBooks(userId, monthStart, nextMonthStart);
		int totalReadCount = countTotalReadBooks(userId);
		int savedToReadCount = countSavedToReadBooks(userId);
		int categoryDiversityCount = countReadCategoryDiversity(userId);
		int recommendationSavedCount = countRecommendationConversions(userId, "SAVE");
		int recommendationReadCount = countRecommendationConversions(userId, "READ");
		int recommendationConversionCount = countRecommendationConversions(userId);
		int purposeMatchReadCount = countPurposeMatchReadBooks(userId);
		int socialActivityScore = countSocialActivityScore(userId, monthStart, nextMonthStart);
		int readingRoomCompletedCount = countCompletedReadingRooms(userId);
		int readingRoomActivityScore = countReadingRoomActivityScore(userId, monthStart, nextMonthStart);
		String topCategory = getTopReadCategory(userId).orElse(null);

		return new ReadingGrowthMetrics(
				monthlyReadCount,
				totalReadCount,
				savedToReadCount,
				categoryDiversityCount,
				recommendationSavedCount,
				recommendationReadCount,
				recommendationConversionCount,
				purposeMatchReadCount,
				socialActivityScore,
				readingRoomCompletedCount,
				readingRoomActivityScore,
				topCategory
		);
	}

	private ReadingGrowthMetrics getReadingGrowthBadgeMetrics(long userId) {
		return new ReadingGrowthMetrics(
				0,
				countTotalReadBooks(userId),
				countSavedToReadBooks(userId),
				countReadCategoryDiversity(userId),
				0,
				0,
				countRecommendationConversions(userId),
				countPurposeMatchReadBooks(userId),
				0,
				countCompletedReadingRooms(userId),
				0,
				null
		);
	}

	private int countSocialActivityScore(long userId, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
		Integer postCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM social_posts sp
				WHERE sp.user_id = ?
					AND sp.status = 'ACTIVE'
					AND sp.created_at >= ?
					AND sp.created_at < ?
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
				""", Integer.class, userId, monthStart, nextMonthStart);
		Integer receivedLikeCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM social_post_likes likes
				JOIN social_posts sp ON sp.id = likes.post_id
				WHERE sp.user_id = ?
					AND sp.status = 'ACTIVE'
					AND likes.created_at >= ?
					AND likes.created_at < ?
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
				""", Integer.class, userId, monthStart, nextMonthStart);
		return Math.min(nullToZero(postCount), 5) * 2
				+ Math.min(nullToZero(receivedLikeCount), 5);
	}

	private int countReadingRoomActivityScore(long userId, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
		return Math.min(countCompletedReadingRooms(userId, monthStart, nextMonthStart) * 2, 6);
	}

	private int countCompletedReadingRooms(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT checkin.session_id)
				FROM reading_room_checkins checkin
				JOIN reading_rooms room ON room.id = checkin.room_id
				WHERE checkin.user_id = ?
					AND room.status <> 'CANCELED'
				""", Integer.class, userId);
		return count == null ? 0 : count;
	}

	private int countCompletedReadingRooms(long userId, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT checkin.session_id)
				FROM reading_room_checkins checkin
				JOIN reading_rooms room ON room.id = checkin.room_id
				WHERE checkin.user_id = ?
					AND checkin.created_at >= ?
					AND checkin.created_at < ?
					AND room.status <> 'CANCELED'
				""", Integer.class, userId, monthStart, nextMonthStart);
		return count == null ? 0 : count;
	}

	private int countMonthlyReadBooks(long userId, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT book_id)
				FROM user_book_interactions
				WHERE user_id = ?
					AND interaction_type = 'READ'
					AND created_at >= ?
					AND created_at < ?
				""", Integer.class, userId, monthStart, nextMonthStart);
		return count == null ? 0 : count;
	}

	private int countTotalReadBooks(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT book_id)
				FROM user_book_interactions
				WHERE user_id = ?
					AND interaction_type = 'READ'
				""", Integer.class, userId);
		return count == null ? 0 : count;
	}

	private int countSavedToReadBooks(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT read_interactions.book_id)
				FROM user_book_interactions read_interactions
				JOIN user_book_interactions save_interactions
					ON save_interactions.user_id = read_interactions.user_id
					AND save_interactions.book_id = read_interactions.book_id
					AND save_interactions.interaction_type = 'SAVE'
					AND (
						save_interactions.created_at < read_interactions.created_at
						OR (
							save_interactions.created_at = read_interactions.created_at
							AND save_interactions.id < read_interactions.id
						)
					)
				WHERE read_interactions.user_id = ?
					AND read_interactions.interaction_type = 'READ'
				""", Integer.class, userId);
		return count == null ? 0 : count;
	}

	private int countReadCategoryDiversity(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT c.name)
				FROM user_book_interactions ubi
				JOIN book_categories bc ON bc.book_id = ubi.book_id
				JOIN categories c ON c.id = bc.category_id
				WHERE ubi.user_id = ?
					AND ubi.interaction_type = 'READ'
					AND c.is_active = TRUE
				""", Integer.class, userId);
		return count == null ? 0 : count;
	}

	private int countRecommendationConversions(long userId, String interactionType) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT r.book_id)
				FROM recommendations r
				JOIN user_book_interactions ubi
					ON ubi.user_id = r.user_id
					AND ubi.book_id = r.book_id
					AND ubi.interaction_type = ?
					AND ubi.created_at >= r.created_at
				WHERE r.user_id = ?
				""", Integer.class, interactionType, userId);
		return count == null ? 0 : count;
	}

	private int countRecommendationConversions(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT r.book_id)
				FROM recommendations r
				JOIN user_book_interactions ubi
					ON ubi.user_id = r.user_id
					AND ubi.book_id = r.book_id
					AND ubi.interaction_type IN ('SAVE', 'READ')
					AND ubi.created_at >= r.created_at
				WHERE r.user_id = ?
				""", Integer.class, userId);
		return count == null ? 0 : count;
	}

	private int countPurposeMatchReadBooks(long userId) {
		List<ReadingPurpose> purposes = getReadingPurposeModels(userId);
		if (purposes.isEmpty()) {
			return 0;
		}

		Set<String> purposeCategories = purposes.stream()
				.flatMap(purpose -> purpose.categoryNames().stream())
				.collect(java.util.stream.Collectors.toSet());
		Set<String> purposeKeywords = purposes.stream()
				.flatMap(purpose -> purpose.keywords().stream())
				.collect(java.util.stream.Collectors.toSet());
		if (purposeCategories.isEmpty() && purposeKeywords.isEmpty()) {
			return 0;
		}

		List<String> matchConditions = new ArrayList<>();
		List<Object> arguments = new ArrayList<>();
		arguments.add(userId);

		if (!purposeCategories.isEmpty()) {
			matchConditions.add("""
					EXISTS (
						SELECT 1
						FROM book_categories bc
						JOIN categories c ON c.id = bc.category_id
						WHERE bc.book_id = ubi.book_id
							AND c.is_active = TRUE
							AND c.name IN (%s)
					)
					""".formatted(placeholders(purposeCategories.size())));
			arguments.addAll(purposeCategories);
		}
		if (!purposeKeywords.isEmpty()) {
			matchConditions.add("""
					EXISTS (
						SELECT 1
						FROM book_keywords bk
						JOIN keywords k ON k.id = bk.keyword_id
						WHERE bk.book_id = ubi.book_id
							AND k.name IN (%s)
					)
					""".formatted(placeholders(purposeKeywords.size())));
			arguments.addAll(purposeKeywords);
		}

		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT ubi.book_id)
				FROM user_book_interactions ubi
				WHERE ubi.user_id = ?
					AND ubi.interaction_type = 'READ'
					AND (%s)
				""".formatted(String.join(" OR ", matchConditions)),
				Integer.class,
				arguments.toArray());
		return count == null ? 0 : count;
	}

	private Optional<String> getTopReadCategory(long userId) {
		List<String> categories = jdbcTemplate.queryForList("""
				SELECT c.name
				FROM user_book_interactions ubi
				JOIN book_categories bc ON bc.book_id = ubi.book_id
				JOIN categories c ON c.id = bc.category_id
				WHERE ubi.user_id = ?
					AND ubi.interaction_type = 'READ'
					AND c.is_active = TRUE
				GROUP BY c.name, c.display_order
				ORDER BY COUNT(DISTINCT ubi.book_id) DESC, c.display_order ASC, c.name ASC
				LIMIT 1
				""", String.class, userId);
		return categories.stream().findFirst();
	}

	private List<ReadingPurpose> getReadingPurposeModels(long userId) {
		return jdbcTemplate.queryForList("""
				SELECT purpose_code
				FROM user_reading_purposes
				WHERE user_id = ?
				ORDER BY created_at ASC, purpose_code ASC
				""", String.class, userId).stream()
				.map(ReadingPurpose::fromCode)
				.flatMap(Optional::stream)
				.toList();
	}

	private List<Badge> getReadingGrowthBadges(ReadingGrowthMetrics metrics) {
		List<Badge> badges = new ArrayList<>();
		if (metrics.totalReadCount() >= 1) {
			badges.add(new Badge("FIRST_READ", "첫 독서 기록", "읽은 책을 1권 이상 기록했습니다."));
		}
		if (metrics.categoryDiversityCount() >= 3) {
			badges.add(new Badge("CATEGORY_EXPLORER", "분야 탐험가", "서로 다른 분야의 책을 3개 이상 읽었습니다."));
		}
		if (metrics.savedToReadCount() >= 1) {
			badges.add(new Badge("SAVED_TO_READ", "저장 후 읽음 실천가", "저장한 책을 읽은 책으로 이어갔습니다."));
		}
		if (metrics.recommendationConversionCount() >= 1) {
			badges.add(new Badge("RECOMMENDATION_FOLLOWER", "추천에서 이어진 책", "추천받은 책을 저장하거나 읽었습니다."));
		}
		if (metrics.purposeMatchReadCount() >= 3) {
			badges.add(new Badge("PURPOSE_MATCH", "목적 맞춤 독서", "선택한 독서 목적과 맞는 책을 3권 이상 읽었습니다."));
		}
		if (metrics.readingRoomCompletedCount() >= 1) {
			badges.add(new Badge("FIRST_READING_ROOM", "첫 모각독 완료", "모각독에 참여하고 종료 후 인증을 완료했습니다."));
		}
		if (metrics.readingRoomCompletedCount() >= 4) {
			badges.add(new Badge("STEADY_READING_ROOM", "꾸준한 모각독", "모각독 인증을 4회 이상 완료했습니다."));
		}
		return badges;
	}

	private Optional<Badge> findBadge(List<Badge> badges, String code) {
		return badges.stream()
				.filter(badge -> code.equals(badge.code()))
				.findFirst();
	}

	private Badge selectPrimaryBadge(List<Badge> badges) {
		return badges.stream()
				.filter(badge -> "PURPOSE_MATCH".equals(badge.code()))
				.findFirst()
				.or(() -> findBadge(badges, "CATEGORY_EXPLORER"))
				.or(() -> findBadge(badges, "SAVED_TO_READ"))
				.or(() -> findBadge(badges, "RECOMMENDATION_FOLLOWER"))
				.or(() -> findBadge(badges, "STEADY_READING_ROOM"))
				.or(() -> findBadge(badges, "FIRST_READING_ROOM"))
				.or(() -> findBadge(badges, "FIRST_READ"))
				.orElse(new Badge(
						"RECORD_START",
						"기록 시작",
						"읽은 책을 추가하면 관심 분야와 독서 목적에 맞춰 성장 흐름을 보여드립니다."
				));
	}

	private int toReadingGrowthLevel(int score) {
		if (score >= 150) {
			return 5;
		}
		if (score >= 100) {
			return 4;
		}
		if (score >= 60) {
			return 3;
		}
		if (score >= 30) {
			return 2;
		}
		return 1;
	}

	private int toReadingGrowthProgress(int score) {
		if (score >= 150) {
			return 100;
		}

		int lowerBound;
		int upperBound;
		if (score >= 100) {
			lowerBound = 100;
			upperBound = 150;
		} else if (score >= 60) {
			lowerBound = 60;
			upperBound = 100;
		} else if (score >= 30) {
			lowerBound = 30;
			upperBound = 60;
		} else {
			lowerBound = 0;
			upperBound = 30;
		}
		return Math.min(100, Math.max(0, (score - lowerBound) * 100 / (upperBound - lowerBound)));
	}

	private String toReadingGrowthSummary(ReadingGrowthMetrics metrics) {
		if (metrics.totalReadCount() == 0) {
			return "읽은 책을 추가하면 관심 분야와 독서 목적에 맞춰 성장 흐름을 보여드립니다.";
		}
		if (metrics.purposeMatchReadCount() >= 3) {
			return "선택한 독서 목적과 맞는 책을 꾸준히 읽고 있습니다.";
		}
		if (metrics.readingRoomCompletedCount() >= 1) {
			return "모각독 인증을 통해 읽기 흐름을 이어가고 있습니다.";
		}
		if (metrics.topCategory() != null && metrics.categoryDiversityCount() >= 2) {
			return metrics.topCategory() + " 분야를 중심으로 여러 분야로 독서 폭을 넓히고 있습니다.";
		}
		if (metrics.topCategory() != null) {
			return metrics.topCategory() + " 분야를 중심으로 독서 취향이 쌓이고 있습니다.";
		}
		return "읽은 책 기록을 바탕으로 독서 성장 흐름을 만들고 있습니다.";
	}

	private MyPageBookResponse mapBook(ResultSet resultSet, int rowNumber) throws SQLException {
		String category = resultSet.getString("category_name");
		return new MyPageBookResponse(
				resultSet.getString("id"),
				resultSet.getString("title"),
				resultSet.getString("author"),
				resultSet.getString("cover_image_url"),
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

	private int nullToZero(Integer value) {
		return value == null ? 0 : value;
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

	private boolean isBadgesPublic(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_privacy_settings
				WHERE user_id = ?
					AND badges_visibility = 'PUBLIC'
				""", Integer.class, userId);
		return count != null && count > 0;
	}

	private record ReadingGrowthMetrics(
			int monthlyReadCount,
			int totalReadCount,
			int savedToReadCount,
			int categoryDiversityCount,
			int recommendationSavedCount,
			int recommendationReadCount,
			int recommendationConversionCount,
			int purposeMatchReadCount,
			int socialActivityScore,
			int readingRoomCompletedCount,
			int readingRoomActivityScore,
			String topCategory
	) {
	}

	public static class OnboardingRequestException extends RuntimeException {

		public OnboardingRequestException(String message) {
			super(message);
		}
	}

	public static class BookInteractionRequestException extends RuntimeException {

		public BookInteractionRequestException(String message) {
			super(message);
		}
	}

	public static class BookInteractionBookNotFoundException extends RuntimeException {

		public BookInteractionBookNotFoundException(String message) {
			super(message);
		}
	}
}
