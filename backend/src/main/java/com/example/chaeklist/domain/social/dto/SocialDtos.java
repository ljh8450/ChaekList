package com.example.chaeklist.domain.social.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class SocialDtos {

	private SocialDtos() {
	}

	public record SocialPostRequest(
			String postType,
			Long bookId,
			Long recommendationId,
			Long sourceInteractionId,
			String content,
			String visibility,
			String idempotencyKey
	) {
	}

	public record SocialPostUpdateRequest(
			String visibility
	) {
	}

	public record SocialPostResponse(
			long id,
			Long userId,
			String nickname,
			boolean authorAnonymized,
			String postType,
			String visibility,
			String status,
			BookSummary book,
			String content,
			int likeCount,
			boolean likedByMe,
			boolean mine,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record BookSummary(
			Long id,
			String title,
			String author,
			String coverImageUrl,
			String category
	) {
	}

	public record LikeResponse(
			long postId,
			boolean liked,
			int likeCount
	) {
	}

	public record ReportRequest(
			String targetType,
			Long targetId,
			String reason,
			String detail
	) {
	}

	public record ReportResponse(
			long id,
			String targetType,
			long targetId,
			String status
	) {
	}

	public record BlockResponse(
			long userId,
			boolean blocked
	) {
	}

	public record PublicProfileResponse(
			long userId,
			String nickname,
			boolean profilePublic,
			boolean growthSummaryPublic,
			String growthSummary,
			int publicPostCount
	) {
	}

	public record PrivacySettingsRequest(
			String readBooksVisibility,
			String savedBooksVisibility,
			String readingGrowthVisibility,
			String badgesVisibility,
			String interestCategoriesVisibility
	) {
	}

	public record PrivacySettingsResponse(
			String readBooksVisibility,
			String savedBooksVisibility,
			String readingGrowthVisibility,
			String badgesVisibility,
			String interestCategoriesVisibility
	) {
	}

	public record NotificationSettingsRequest(
			Boolean likeNotificationsEnabled,
			Boolean reportStatusNotificationsEnabled,
			Boolean serviceNotificationsEnabled
	) {
	}

	public record NotificationSettingsResponse(
			boolean likeNotificationsEnabled,
			boolean reportStatusNotificationsEnabled,
			boolean serviceNotificationsEnabled
	) {
	}

	public record SettingsResponse(
			PrivacySettingsResponse privacy,
			NotificationSettingsResponse notifications
	) {
	}

	public record SearchResponse(
			String query,
			String type,
			List<SearchSection> sections
	) {
	}

	public record SearchSection(
			String type,
			List<SearchItem> items
	) {
	}

	public record SearchItem(
			String id,
			String type,
			String title,
			String summary,
			String detailPath
	) {
	}
}
