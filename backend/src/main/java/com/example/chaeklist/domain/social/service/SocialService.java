package com.example.chaeklist.domain.social.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.example.chaeklist.domain.mypage.dto.ReadingGrowthResponse.Badge;
import com.example.chaeklist.domain.mypage.service.MyPageService;
import com.example.chaeklist.domain.social.dto.SocialDtos.BlockResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.BookSummary;
import com.example.chaeklist.domain.social.dto.SocialDtos.LikeResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.NotificationResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.NotificationSettingsRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.NotificationSettingsResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.PrivacySettingsRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.PrivacySettingsResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.PublicProfileResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.ReportRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.ReportResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.SettingsResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostUpdateRequest;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;
	private static final Set<String> POST_TYPES = Set.of("READ_BOOK", "SAVED_BOOK", "RECOMMENDED_BOOK", "READING_GROWTH", "BADGE", "TEXT");
	private static final Set<String> VISIBILITIES = Set.of("PRIVATE", "PUBLIC");
	private static final Set<String> REPORT_TARGET_TYPES = Set.of("POST", "USER_NICKNAME");
	private static final Set<String> REPORT_REASONS = Set.of("SPAM", "ABUSE", "INAPPROPRIATE_NICKNAME", "INAPPROPRIATE_CONTENT", "OTHER");
	private static final Set<String> FEED_SORTS = Set.of("LATEST", "LIKES");

	private final JdbcTemplate jdbcTemplate;
	private final MyPageService myPageService;

	public SocialService(JdbcTemplate jdbcTemplate, MyPageService myPageService) {
		this.jdbcTemplate = jdbcTemplate;
		this.myPageService = myPageService;
	}

	public List<SocialPostResponse> getFeed(AuthenticatedUser user, String type, String sort, int limit) {
		String normalizedType = normalizeOptionalPostType(type);
		String orderBy = feedOrderBy(sort);
		return jdbcTemplate.query("""
				SELECT %s
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND (? IS NULL OR sp.post_type = ?)
					AND (sp.user_id IS NULL OR u.status = 'ACTIVE')
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
					AND (? IS NULL OR sp.user_id IS NULL OR NOT EXISTS (
						SELECT 1
						FROM user_blocks block
						WHERE block.blocker_user_id = ?
							AND block.blocked_user_id = sp.user_id
					))
				ORDER BY %s
				LIMIT ?
				""".formatted(postSelectColumns(), primaryCategorySubquery(), orderBy),
				this::mapPost,
				nullableUserId(user),
				nullableUserId(user),
				nullableUserId(user),
				nullableUserId(user),
				normalizedType,
				normalizedType,
				nullableUserId(user),
				nullableUserId(user),
				normalizeLimit(limit)
		);
	}

	public List<SocialPostResponse> getMyPosts(AuthenticatedUser user, int limit) {
		return jdbcTemplate.query("""
				SELECT %s
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE sp.user_id = ?
					AND sp.status = 'ACTIVE'
				ORDER BY sp.created_at DESC, sp.id DESC
				LIMIT ?
				""".formatted(postSelectColumns(), primaryCategorySubquery()),
				this::mapPost,
				user.id(),
				user.id(),
				user.id(),
				user.id(),
				user.id(),
				normalizeLimit(limit)
		);
	}

	public List<SocialPostResponse> getLikedPosts(AuthenticatedUser user, int limit) {
		return jdbcTemplate.query("""
				SELECT %s
				FROM social_post_likes liked_posts
				JOIN social_posts sp ON sp.id = liked_posts.post_id
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE liked_posts.user_id = ?
					AND sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND (sp.user_id IS NULL OR u.status = 'ACTIVE')
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
				ORDER BY liked_posts.created_at DESC, sp.id DESC
				LIMIT ?
				""".formatted(postSelectColumns(), primaryCategorySubquery()),
				this::mapPost,
				user.id(),
				user.id(),
				user.id(),
				user.id(),
				user.id(),
				normalizeLimit(limit)
		);
	}

	public List<SocialPostResponse> getPublicProfilePosts(AuthenticatedUser user, long userId, String type, int limit) {
		getPublicProfile(userId);
		String normalizedType = normalizeOptionalPostType(type);
		if (normalizedType == null) {
			return jdbcTemplate.query("""
					SELECT %s
					FROM social_posts sp
					LEFT JOIN users u ON u.id = sp.user_id
					LEFT JOIN books b ON b.id = sp.book_id
					LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
					WHERE sp.user_id = ?
						AND sp.visibility = 'PUBLIC'
						AND sp.status = 'ACTIVE'
						AND u.status = 'ACTIVE'
						AND NOT EXISTS (
							SELECT 1
							FROM social_admin_hidden_posts hidden
							WHERE hidden.post_id = sp.id
						)
					ORDER BY sp.created_at DESC, sp.id DESC
					LIMIT ?
					""".formatted(postSelectColumns(), primaryCategorySubquery()),
					this::mapPost,
					nullableUserId(user),
					nullableUserId(user),
					nullableUserId(user),
					nullableUserId(user),
					userId,
					normalizeLimit(limit)
			);
		}
		return jdbcTemplate.query("""
				SELECT %s
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE sp.user_id = ?
					AND sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND sp.post_type = ?
					AND u.status = 'ACTIVE'
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
				ORDER BY sp.created_at DESC, sp.id DESC
				LIMIT ?
				""".formatted(postSelectColumns(), primaryCategorySubquery()),
				this::mapPost,
				nullableUserId(user),
				nullableUserId(user),
				nullableUserId(user),
				nullableUserId(user),
				userId,
				normalizedType,
				normalizeLimit(limit)
		);
	}

	@Transactional
	public SocialPostResponse createPost(AuthenticatedUser user, SocialPostRequest request) {
		String postType = normalizePostType(request == null ? null : request.postType());
		String visibility = normalizeVisibility(request == null ? null : request.visibility(), "PRIVATE");
		String content = normalizeContent(request == null ? null : request.content(), postType);
		Long bookId = request == null ? null : request.bookId();
		Long recommendationId = request == null ? null : request.recommendationId();
		Long sourceInteractionId = request == null ? null : request.sourceInteractionId();
		String idempotencyKey = normalizeBlankToNull(request == null ? null : request.idempotencyKey());

		validatePostSource(user.id(), postType, bookId, recommendationId, sourceInteractionId);
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				java.sql.PreparedStatement statement = connection.prepareStatement("""
						INSERT INTO social_posts (
							user_id, author_snapshot_nickname, post_type, visibility, status,
							book_id, recommendation_id, source_interaction_id, content, idempotency_key,
							created_at, updated_at
						)
						VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
						""", java.sql.Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, user.id());
				statement.setString(2, user.nickname());
				statement.setString(3, postType);
				statement.setString(4, visibility);
				setNullableLong(statement, 5, bookId);
				setNullableLong(statement, 6, recommendationId);
				setNullableLong(statement, 7, sourceInteractionId);
				statement.setString(8, content);
				statement.setString(9, idempotencyKey);
				return statement;
			}, keyHolder);
			Number key = generatedId(keyHolder);
			if (key == null) {
				throw new SocialRequestException("Post creation failed.");
			}
			return getPostForOwner(key.longValue(), user.id());
		} catch (DuplicateKeyException exception) {
			if (idempotencyKey == null) {
				throw exception;
			}
			return findPostByIdempotencyKey(user.id(), idempotencyKey)
					.orElseThrow(() -> new SocialRequestException("Post creation conflict."));
		}
	}

	@Transactional
	public SocialPostResponse updatePost(AuthenticatedUser user, long postId, SocialPostUpdateRequest request) {
		String visibility = normalizeVisibility(request == null ? null : request.visibility(), null);
		int updated = jdbcTemplate.update("""
				UPDATE social_posts
				SET visibility = ?,
					updated_at = CURRENT_TIMESTAMP(6)
				WHERE id = ?
					AND user_id = ?
					AND status = 'ACTIVE'
				""", visibility, postId, user.id());
		if (updated == 0) {
			throw new SocialNotFoundException("Post not found.");
		}
		return getPostForOwner(postId, user.id());
	}

	@Transactional
	public void deletePost(AuthenticatedUser user, long postId) {
		int updated = jdbcTemplate.update("""
				UPDATE social_posts
				SET status = 'DELETED',
					visibility = 'PRIVATE',
					updated_at = CURRENT_TIMESTAMP(6)
				WHERE id = ?
					AND user_id = ?
					AND status = 'ACTIVE'
				""", postId, user.id());
		if (updated == 0) {
			throw new SocialNotFoundException("Post not found.");
		}
	}

	@Transactional
	public LikeResponse likePost(AuthenticatedUser user, long postId) {
		validatePublicActivePost(postId);
		if (hasLikedPost(postId, user.id())) {
			return new LikeResponse(postId, true, countLikes(postId));
		}
		try {
			jdbcTemplate.update("""
					INSERT INTO social_post_likes (post_id, user_id, created_at)
					VALUES (?, ?, CURRENT_TIMESTAMP(6))
					""", postId, user.id());
			createLikeNotification(postId, user);
		} catch (DuplicateKeyException ignored) {
		}
		return new LikeResponse(postId, true, countLikes(postId));
	}

	@Transactional
	public LikeResponse unlikePost(AuthenticatedUser user, long postId) {
		validatePublicActivePost(postId);
		jdbcTemplate.update("""
				DELETE FROM social_post_likes
				WHERE post_id = ?
					AND user_id = ?
				""", postId, user.id());
		return new LikeResponse(postId, false, countLikes(postId));
	}

	public PublicProfileResponse getPublicProfile(long userId) {
		return jdbcTemplate.queryForObject("""
				SELECT
					u.id,
					u.nickname,
					COALESCE(upp.profile_public, FALSE) AS profile_public,
					COALESCE(upp.growth_summary_public, FALSE) AS growth_summary_public,
					COUNT(DISTINCT sp.id) AS public_post_count
				FROM users u
				LEFT JOIN user_public_profiles upp ON upp.user_id = u.id
				LEFT JOIN social_posts sp
					ON sp.user_id = u.id
					AND sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND NOT EXISTS (
						SELECT 1
						FROM social_admin_hidden_posts hidden
						WHERE hidden.post_id = sp.id
					)
				WHERE u.id = ?
					AND u.status = 'ACTIVE'
				GROUP BY u.id, u.nickname, upp.profile_public, upp.growth_summary_public
				""",
				(resultSet, rowNumber) -> {
					boolean profilePublic = resultSet.getBoolean("profile_public");
					int publicPostCount = resultSet.getInt("public_post_count");
					boolean hasPublicProfile = profilePublic || publicPostCount > 0;
					if (!hasPublicProfile) {
						throw new SocialNotFoundException("Public profile not found.");
					}
					boolean growthPublic = resultSet.getBoolean("growth_summary_public");
					return new PublicProfileResponse(
							resultSet.getLong("id"),
							resultSet.getString("nickname"),
							hasPublicProfile,
							growthPublic,
							growthPublic ? "공개된 독서 성장 요약입니다." : null,
							publicPostCount,
							publicPrimaryBadge(resultSet.getLong("id"))
					);
				},
				userId
		);
	}

	public SettingsResponse getSettings(AuthenticatedUser user) {
		return new SettingsResponse(getPrivacySettings(user), getNotificationSettings(user));
	}

	public PrivacySettingsResponse getPrivacySettings(AuthenticatedUser user) {
		ensurePrivacySettings(user.id());
		return jdbcTemplate.queryForObject("""
				SELECT read_books_visibility, saved_books_visibility, reading_growth_visibility,
					badges_visibility, interest_categories_visibility
				FROM user_privacy_settings
				WHERE user_id = ?
				""",
				(resultSet, rowNumber) -> new PrivacySettingsResponse(
						resultSet.getString("read_books_visibility"),
						resultSet.getString("saved_books_visibility"),
						resultSet.getString("reading_growth_visibility"),
						resultSet.getString("badges_visibility"),
						resultSet.getString("interest_categories_visibility")
				),
				user.id()
		);
	}

	@Transactional
	public PrivacySettingsResponse updatePrivacySettings(AuthenticatedUser user, PrivacySettingsRequest request) {
		PrivacySettingsResponse current = getPrivacySettings(user);
		String readBooks = normalizeVisibility(request == null ? null : request.readBooksVisibility(), current.readBooksVisibility());
		String savedBooks = normalizeVisibility(request == null ? null : request.savedBooksVisibility(), current.savedBooksVisibility());
		String readingGrowth = normalizeVisibility(request == null ? null : request.readingGrowthVisibility(), current.readingGrowthVisibility());
		String badges = normalizeVisibility(request == null ? null : request.badgesVisibility(), current.badgesVisibility());
		String interests = normalizeInterestVisibility(request == null ? null : request.interestCategoriesVisibility(), current.interestCategoriesVisibility());
		jdbcTemplate.update("""
				INSERT INTO user_privacy_settings (
					user_id, read_books_visibility, saved_books_visibility, reading_growth_visibility,
					badges_visibility, interest_categories_visibility, created_at, updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
				ON DUPLICATE KEY UPDATE
					read_books_visibility = VALUES(read_books_visibility),
					saved_books_visibility = VALUES(saved_books_visibility),
					reading_growth_visibility = VALUES(reading_growth_visibility),
					badges_visibility = VALUES(badges_visibility),
					interest_categories_visibility = VALUES(interest_categories_visibility),
					updated_at = CURRENT_TIMESTAMP(6)
				""", user.id(), readBooks, savedBooks, readingGrowth, badges, interests);
		upsertPublicProfile(user.id(), "PUBLIC".equals(readingGrowth));
		return getPrivacySettings(user);
	}

	public NotificationSettingsResponse getNotificationSettings(AuthenticatedUser user) {
		ensureNotificationSettings(user.id());
		return jdbcTemplate.queryForObject("""
				SELECT like_notifications_enabled, report_status_notifications_enabled, service_notifications_enabled
				FROM user_notification_settings
				WHERE user_id = ?
				""",
				(resultSet, rowNumber) -> new NotificationSettingsResponse(
						resultSet.getBoolean("like_notifications_enabled"),
						resultSet.getBoolean("report_status_notifications_enabled"),
						resultSet.getBoolean("service_notifications_enabled")
				),
				user.id()
		);
	}

	public List<NotificationResponse> getNotifications(AuthenticatedUser user, int limit) {
		return jdbcTemplate.query("""
				SELECT id, notification_type, target_type, target_id, title, message, read_at, created_at
				FROM user_notifications
				WHERE user_id = ?
				ORDER BY created_at DESC, id DESC
				LIMIT ?
				""",
				this::mapNotification,
				user.id(),
				normalizeLimit(limit)
		);
	}

	@Transactional
	public NotificationResponse markNotificationRead(AuthenticatedUser user, long notificationId) {
		int updated = jdbcTemplate.update("""
				UPDATE user_notifications
				SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP(6))
				WHERE id = ?
					AND user_id = ?
				""", notificationId, user.id());
		if (updated == 0) {
			throw new SocialNotFoundException("Notification not found.");
		}
		return getNotification(user.id(), notificationId);
	}

	@Transactional
	public NotificationSettingsResponse updateNotificationSettings(AuthenticatedUser user, NotificationSettingsRequest request) {
		NotificationSettingsResponse current = getNotificationSettings(user);
		boolean like = request == null || request.likeNotificationsEnabled() == null
				? current.likeNotificationsEnabled()
				: request.likeNotificationsEnabled();
		boolean report = request == null || request.reportStatusNotificationsEnabled() == null
				? current.reportStatusNotificationsEnabled()
				: request.reportStatusNotificationsEnabled();
		boolean service = request == null || request.serviceNotificationsEnabled() == null
				? current.serviceNotificationsEnabled()
				: request.serviceNotificationsEnabled();
		jdbcTemplate.update("""
				INSERT INTO user_notification_settings (
					user_id, like_notifications_enabled, report_status_notifications_enabled,
					service_notifications_enabled, created_at, updated_at
				)
				VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
				ON DUPLICATE KEY UPDATE
					like_notifications_enabled = VALUES(like_notifications_enabled),
					report_status_notifications_enabled = VALUES(report_status_notifications_enabled),
					service_notifications_enabled = VALUES(service_notifications_enabled),
					updated_at = CURRENT_TIMESTAMP(6)
				""", user.id(), like, report, service);
		return getNotificationSettings(user);
	}

	@Transactional
	public void withdraw(AuthenticatedUser user) {
		jdbcTemplate.update("""
				UPDATE social_posts
				SET author_snapshot_nickname = '탈퇴한 사용자',
					author_anonymized = TRUE,
					user_id = NULL,
					updated_at = CURRENT_TIMESTAMP(6)
				WHERE user_id = ?
				""", user.id());
		jdbcTemplate.update("""
				UPDATE users
				SET email = CONCAT('deleted_', id, '@deleted.local'),
					nickname = CONCAT('deleted_', id),
					password_hash = '',
					status = 'DELETED',
					updated_at = CURRENT_TIMESTAMP(6)
				WHERE id = ?
				""", user.id());
	}

	@Transactional
	public ReportResponse report(AuthenticatedUser user, ReportRequest request) {
		String targetType = normalizeReportTargetType(request == null ? null : request.targetType());
		long targetId = normalizePositiveId(request == null ? null : request.targetId(), "Target id is required.");
		String reason = normalizeReportReason(request == null ? null : request.reason());
		String detail = truncate(normalizeBlankToNull(request == null ? null : request.detail()), 500);
		validateReportTarget(targetType, targetId);
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				java.sql.PreparedStatement statement = connection.prepareStatement("""
						INSERT INTO social_reports (
							reporter_user_id, target_type, target_id, reason, detail, status, created_at, updated_at
						)
						VALUES (?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
						""", java.sql.Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, user.id());
				statement.setString(2, targetType);
				statement.setLong(3, targetId);
				statement.setString(4, reason);
				statement.setString(5, detail);
				return statement;
			}, keyHolder);
			Number key = generatedId(keyHolder);
			return new ReportResponse(key == null ? 0 : key.longValue(), targetType, targetId, "PENDING");
		} catch (DuplicateKeyException ignored) {
			return jdbcTemplate.queryForObject("""
					SELECT id, target_type, target_id, status
					FROM social_reports
					WHERE reporter_user_id = ?
						AND target_type = ?
						AND target_id = ?
					""",
					(resultSet, rowNumber) -> new ReportResponse(
							resultSet.getLong("id"),
							resultSet.getString("target_type"),
							resultSet.getLong("target_id"),
							resultSet.getString("status")
					),
					user.id(),
					targetType,
					targetId
			);
		}
	}

	@Transactional
	public BlockResponse block(AuthenticatedUser user, long blockedUserId) {
		if (user.id() == blockedUserId) {
			throw new SocialRequestException("Cannot block yourself.");
		}
		validateActiveUser(blockedUserId);
		try {
			jdbcTemplate.update("""
					INSERT INTO user_blocks (blocker_user_id, blocked_user_id, created_at)
					VALUES (?, ?, CURRENT_TIMESTAMP(6))
					""", user.id(), blockedUserId);
		} catch (DuplicateKeyException ignored) {
		}
		return new BlockResponse(blockedUserId, true);
	}

	@Transactional
	public BlockResponse unblock(AuthenticatedUser user, long blockedUserId) {
		jdbcTemplate.update("""
				DELETE FROM user_blocks
				WHERE blocker_user_id = ?
					AND blocked_user_id = ?
				""", user.id(), blockedUserId);
		return new BlockResponse(blockedUserId, false);
	}

	private SocialPostResponse getPostForOwner(long postId, long userId) {
		return jdbcTemplate.queryForObject("""
				SELECT %s
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE sp.id = ?
					AND (sp.user_id = ? OR sp.user_id IS NULL)
				""".formatted(postSelectColumns(), primaryCategorySubquery()),
				this::mapPost,
				userId,
				userId,
				userId,
				userId,
				postId,
				userId
		);
	}

	private Optional<SocialPostResponse> findPostByIdempotencyKey(long userId, String idempotencyKey) {
		List<SocialPostResponse> posts = jdbcTemplate.query("""
				SELECT %s
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				LEFT JOIN books b ON b.id = sp.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE sp.user_id = ?
					AND sp.idempotency_key = ?
				ORDER BY sp.id DESC
				LIMIT 1
				""".formatted(postSelectColumns(), primaryCategorySubquery()),
				this::mapPost,
				userId,
				userId,
				userId,
				userId,
				userId,
				idempotencyKey
		);
		return posts.stream().findFirst();
	}

	private void validatePostSource(long userId, String postType, Long bookId, Long recommendationId, Long sourceInteractionId) {
		switch (postType) {
			case "READ_BOOK" -> validateInteractionOwner(userId, bookId, sourceInteractionId, "READ");
			case "SAVED_BOOK" -> validateSavedBook(userId, bookId, sourceInteractionId);
			case "RECOMMENDED_BOOK" -> validateRecommendationOwner(userId, recommendationId, bookId);
			case "READING_GROWTH", "BADGE", "TEXT" -> {
			}
			default -> throw new SocialRequestException("Unsupported post type.");
		}
	}

	private void validateInteractionOwner(long userId, Long bookId, Long sourceInteractionId, String interactionType) {
		if (bookId == null) {
			throw new SocialRequestException("Book id is required.");
		}
		Integer count;
		if (sourceInteractionId == null) {
			count = jdbcTemplate.queryForObject("""
					SELECT COUNT(*)
					FROM user_book_interactions
					WHERE user_id = ?
						AND book_id = ?
						AND interaction_type = ?
					""", Integer.class, userId, bookId, interactionType);
		} else {
			count = jdbcTemplate.queryForObject("""
					SELECT COUNT(*)
					FROM user_book_interactions
					WHERE id = ?
						AND user_id = ?
						AND book_id = ?
						AND interaction_type = ?
					""", Integer.class, sourceInteractionId, userId, bookId, interactionType);
		}
		if (count == null || count == 0) {
			throw new SocialRequestException("Post source is not owned by user.");
		}
	}

	private void validateSavedBook(long userId, Long bookId, Long sourceInteractionId) {
		validateInteractionOwner(userId, bookId, sourceInteractionId, "SAVE");
		Integer currentSaves = jdbcTemplate.queryForObject("""
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
		if (currentSaves == null || currentSaves == 0) {
			throw new SocialRequestException("Book is not currently saved.");
		}
	}

	private void validateRecommendationOwner(long userId, Long recommendationId, Long bookId) {
		if (recommendationId == null) {
			throw new SocialRequestException("Recommendation id is required.");
		}
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM recommendations
				WHERE id = ?
					AND user_id = ?
					AND (? IS NULL OR book_id = ?)
				""", Integer.class, recommendationId, userId, bookId, bookId);
		if (count == null || count == 0) {
			throw new SocialRequestException("Recommendation is not owned by user.");
		}
	}

	private void validatePublicActivePost(long postId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM social_posts sp
				LEFT JOIN users u ON u.id = sp.user_id
				WHERE sp.id = ?
					AND sp.visibility = 'PUBLIC'
					AND sp.status = 'ACTIVE'
					AND (sp.user_id IS NULL OR u.status = 'ACTIVE')
				""", Integer.class, postId);
		if (count == null || count == 0) {
			throw new SocialNotFoundException("Post not found.");
		}
	}

	private void validateReportTarget(String targetType, long targetId) {
		if ("POST".equals(targetType)) {
			validatePublicActivePost(targetId);
			return;
		}
		validateActiveUser(targetId);
	}

	private void validateActiveUser(long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM users
				WHERE id = ?
					AND status = 'ACTIVE'
				""", Integer.class, userId);
		if (count == null || count == 0) {
			throw new SocialNotFoundException("User not found.");
		}
	}

	private void ensurePrivacySettings(long userId) {
		jdbcTemplate.update("""
				INSERT IGNORE INTO user_privacy_settings (
					user_id, read_books_visibility, saved_books_visibility, reading_growth_visibility,
					badges_visibility, interest_categories_visibility, created_at, updated_at
				)
				VALUES (?, 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
				""", userId);
	}

	private void ensureNotificationSettings(long userId) {
		jdbcTemplate.update("""
				INSERT IGNORE INTO user_notification_settings (
					user_id, like_notifications_enabled, report_status_notifications_enabled,
					service_notifications_enabled, created_at, updated_at
				)
				VALUES (?, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
				""", userId);
	}

	private void upsertPublicProfile(long userId, boolean growthSummaryPublic) {
		jdbcTemplate.update("""
				INSERT INTO user_public_profiles (
					user_id, profile_public, growth_summary_public, created_at, updated_at
				)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
				ON DUPLICATE KEY UPDATE
					profile_public = VALUES(profile_public),
					growth_summary_public = VALUES(growth_summary_public),
					updated_at = CURRENT_TIMESTAMP(6)
				""", userId, growthSummaryPublic, growthSummaryPublic);
	}

	private int countLikes(long postId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM social_post_likes
				WHERE post_id = ?
				""", Integer.class, postId);
		return count == null ? 0 : count;
	}

	private boolean hasLikedPost(long postId, long userId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM social_post_likes
				WHERE post_id = ?
					AND user_id = ?
				""", Integer.class, postId, userId);
		return count != null && count > 0;
	}

	private void createLikeNotification(long postId, AuthenticatedUser liker) {
		LikeNotificationTarget target = findLikeNotificationTarget(postId).orElse(null);
		if (target == null || target.userId() == liker.id() || !isLikeNotificationEnabled(target.userId())) {
			return;
		}
		jdbcTemplate.update("""
				INSERT INTO user_notifications (
					user_id, notification_type, target_type, target_id, title, message, created_at
				)
				VALUES (?, 'LIKE', 'POST', ?, ?, ?, CURRENT_TIMESTAMP(6))
				""",
				target.userId(),
				postId,
				"게시글에 좋아요가 추가되었습니다.",
				liker.nickname() + "님이 회원님의 게시글을 좋아합니다."
		);
	}

	private Optional<LikeNotificationTarget> findLikeNotificationTarget(long postId) {
		List<LikeNotificationTarget> targets = jdbcTemplate.query("""
				SELECT user_id
				FROM social_posts
				WHERE id = ?
					AND user_id IS NOT NULL
				""",
				(resultSet, rowNumber) -> new LikeNotificationTarget(resultSet.getLong("user_id")),
				postId
		);
		return targets.stream().findFirst();
	}

	private boolean isLikeNotificationEnabled(long userId) {
		ensureNotificationSettings(userId);
		Boolean enabled = jdbcTemplate.queryForObject("""
				SELECT like_notifications_enabled
				FROM user_notification_settings
				WHERE user_id = ?
				""", Boolean.class, userId);
		return Boolean.TRUE.equals(enabled);
	}

	private NotificationResponse getNotification(long userId, long notificationId) {
		return jdbcTemplate.queryForObject("""
				SELECT id, notification_type, target_type, target_id, title, message, read_at, created_at
				FROM user_notifications
				WHERE id = ?
					AND user_id = ?
				""",
				this::mapNotification,
				notificationId,
				userId
		);
	}

	private NotificationResponse mapNotification(ResultSet resultSet, int rowNumber) throws SQLException {
		LocalDateTime readAt = readLocalDateTime(resultSet, "read_at");
		return new NotificationResponse(
				resultSet.getLong("id"),
				resultSet.getString("notification_type"),
				resultSet.getString("target_type"),
				resultSet.getLong("target_id"),
				resultSet.getString("title"),
				resultSet.getString("message"),
				readAt != null,
				readAt,
				readLocalDateTime(resultSet, "created_at")
		);
	}

	private SocialPostResponse mapPost(ResultSet resultSet, int rowNumber) throws SQLException {
		Long authorUserId = getNullableLong(resultSet, "user_id");
		Long bookId = getNullableLong(resultSet, "book_id");
		BookSummary book = bookId == null ? null : new BookSummary(
				bookId,
				resultSet.getString("book_title"),
				resultSet.getString("book_author"),
				resultSet.getString("cover_image_url"),
				resultSet.getString("category_name")
		);
		return new SocialPostResponse(
				resultSet.getLong("id"),
				authorUserId,
				resultSet.getString("display_nickname"),
				resultSet.getBoolean("author_anonymized"),
				resultSet.getString("post_type"),
				resultSet.getString("visibility"),
				resultSet.getString("status"),
				book,
				resultSet.getString("content"),
				resultSet.getInt("like_count"),
				resultSet.getBoolean("liked_by_me"),
				resultSet.getBoolean("mine"),
				readLocalDateTime(resultSet, "created_at"),
				readLocalDateTime(resultSet, "updated_at"),
				publicPrimaryBadge(authorUserId)
		);
	}

	private Badge publicPrimaryBadge(Long userId) {
		if (userId == null) {
			return null;
		}
		return myPageService.getPublicPrimaryReadingGrowthBadge(userId);
	}

	private String postSelectColumns() {
		return """
				sp.id,
				sp.user_id,
				CASE
					WHEN sp.author_anonymized = TRUE THEN COALESCE(sp.author_snapshot_nickname, '탈퇴한 사용자')
					ELSE COALESCE(u.nickname, sp.author_snapshot_nickname, '탈퇴한 사용자')
				END AS display_nickname,
				sp.author_anonymized,
				sp.post_type,
				sp.visibility,
				sp.status,
				sp.book_id,
				b.title AS book_title,
				b.author AS book_author,
				b.cover_image_url,
				primary_category.category_name,
				sp.content,
				(
					SELECT COUNT(*)
					FROM social_post_likes likes
					WHERE likes.post_id = sp.id
				) AS like_count,
				CASE
					WHEN ? IS NULL THEN FALSE
					ELSE EXISTS (
						SELECT 1
						FROM social_post_likes my_like
						WHERE my_like.post_id = sp.id
							AND my_like.user_id = ?
					)
				END AS liked_by_me,
				CASE
					WHEN ? IS NULL THEN FALSE
					WHEN sp.user_id = ? THEN TRUE
					ELSE FALSE
				END AS mine,
				sp.created_at,
				sp.updated_at
				""";
	}

	private String primaryCategorySubquery() {
		return """
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
				""";
	}

	private String normalizePostType(String postType) {
		String normalized = normalizeRequiredCode(postType, "Post type is required.");
		if (!POST_TYPES.contains(normalized)) {
			throw new SocialRequestException("Unsupported post type.");
		}
		return normalized;
	}

	private String normalizeOptionalPostType(String postType) {
		if (postType == null || postType.isBlank() || "ALL".equalsIgnoreCase(postType)) {
			return null;
		}
		return normalizePostType(postType);
	}

	private String feedOrderBy(String sort) {
		String normalized = normalizeFeedSort(sort);
		if ("LIKES".equals(normalized)) {
			return "like_count DESC, sp.created_at DESC, sp.id DESC";
		}
		return "sp.created_at DESC, sp.id DESC";
	}

	private String normalizeFeedSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return "LATEST";
		}
		String normalized = sort.trim().toUpperCase(Locale.ROOT);
		if (!FEED_SORTS.contains(normalized)) {
			throw new SocialRequestException("Unsupported feed sort.");
		}
		return normalized;
	}

	private String normalizeVisibility(String visibility, String defaultValue) {
		if (visibility == null || visibility.isBlank()) {
			if (defaultValue == null) {
				throw new SocialRequestException("Visibility is required.");
			}
			return defaultValue;
		}
		String normalized = visibility.trim().toUpperCase(Locale.ROOT);
		if (!VISIBILITIES.contains(normalized)) {
			throw new SocialRequestException("Unsupported visibility.");
		}
		return normalized;
	}

	private String normalizeInterestVisibility(String visibility, String defaultValue) {
		if (visibility == null || visibility.isBlank()) {
			return defaultValue;
		}
		String normalized = visibility.trim().toUpperCase(Locale.ROOT);
		if (!Set.of("PRIVATE", "PARTIAL", "PUBLIC").contains(normalized)) {
			throw new SocialRequestException("Unsupported interest visibility.");
		}
		return normalized;
	}

	private String normalizeContent(String content, String postType) {
		String normalized = truncate(normalizeBlankToNull(content), 1000);
		if ("TEXT".equals(postType) && normalized == null) {
			throw new SocialRequestException("Content is required for text posts.");
		}
		return normalized;
	}

	private String normalizeReportTargetType(String targetType) {
		String normalized = normalizeRequiredCode(targetType, "Target type is required.");
		if (!REPORT_TARGET_TYPES.contains(normalized)) {
			throw new SocialRequestException("Unsupported report target type.");
		}
		return normalized;
	}

	private String normalizeReportReason(String reason) {
		String normalized = normalizeRequiredCode(reason, "Report reason is required.");
		if (!REPORT_REASONS.contains(normalized)) {
			throw new SocialRequestException("Unsupported report reason.");
		}
		return normalized;
	}

	private String normalizeRequiredCode(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new SocialRequestException(message);
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private long normalizePositiveId(Long id, String message) {
		if (id == null || id <= 0) {
			throw new SocialRequestException(message);
		}
		return id;
	}

	private int normalizeLimit(int limit) {
		if (limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private String normalizeBlankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private Long nullableUserId(AuthenticatedUser user) {
		return user == null ? null : user.id();
	}

	private Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
		long value = resultSet.getLong(columnName);
		return resultSet.wasNull() ? null : value;
	}

	private void setNullableLong(java.sql.PreparedStatement statement, int parameterIndex, Long value) throws SQLException {
		if (value == null) {
			statement.setNull(parameterIndex, java.sql.Types.BIGINT);
			return;
		}
		statement.setLong(parameterIndex, value);
	}

	private Number generatedId(KeyHolder keyHolder) {
		if (keyHolder.getKeyList().isEmpty()) {
			return null;
		}
		Object id = keyHolder.getKeyList().getFirst().get("id");
		if (id instanceof Number number) {
			return number;
		}
		if (keyHolder.getKeyList().getFirst().size() == 1) {
			Object onlyValue = keyHolder.getKeyList().getFirst().values().iterator().next();
			return onlyValue instanceof Number number ? number : null;
		}
		return null;
	}

	private LocalDateTime readLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
		java.sql.Timestamp timestamp = resultSet.getTimestamp(columnName);
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	public static class SocialRequestException extends RuntimeException {

		public SocialRequestException(String message) {
			super(message);
		}
	}

	public static class SocialNotFoundException extends RuntimeException {

		public SocialNotFoundException(String message) {
			super(message);
		}
	}

	private record LikeNotificationTarget(long userId) {
	}
}
