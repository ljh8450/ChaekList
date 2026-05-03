package com.example.chaeklist.domain.social.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.chaeklist.domain.mypage.dto.ReadingGrowthResponse.Badge;

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
			LocalDateTime updatedAt,
			Badge primaryBadge,
			List<SocialPostMediaResponse> media
	) {
		public SocialPostResponse {
			media = media == null ? List.of() : List.copyOf(media);
		}

		public SocialPostResponse(
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
			this(id, userId, nickname, authorAnonymized, postType, visibility, status, book, content, likeCount, likedByMe, mine, createdAt, updatedAt, null, List.of());
		}

		public SocialPostResponse withPrimaryBadge(Badge badge) {
			return new SocialPostResponse(id, userId, nickname, authorAnonymized, postType, visibility, status, book, content, likeCount, likedByMe, mine, createdAt, updatedAt, badge, media);
		}
	}

	public record SocialPostMediaResponse(
			long id,
			long postId,
			String fileName,
			String contentType,
			long sizeBytes,
			int sortOrder,
			String url,
			LocalDateTime createdAt
	) {
	}

	public record SocialPostMediaContent(
			String fileName,
			String contentType,
			long sizeBytes,
			byte[] data
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
			int publicPostCount,
			Badge primaryBadge
	) {
		public PublicProfileResponse(
				long userId,
				String nickname,
				boolean profilePublic,
				boolean growthSummaryPublic,
				String growthSummary,
				int publicPostCount
		) {
			this(userId, nickname, profilePublic, growthSummaryPublic, growthSummary, publicPostCount, null);
		}
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

	public record NotificationResponse(
			long id,
			String notificationType,
			String targetType,
			long targetId,
			String title,
			String message,
			boolean read,
			LocalDateTime readAt,
			LocalDateTime createdAt
	) {
	}

	public record AdminReportResponse(
			long id,
			long reporterUserId,
			String reporterNickname,
			String targetType,
			long targetId,
			String reason,
			String detail,
			String status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record AdminReportStatusRequest(
			String status,
			String memo,
			String nicknameAction
	) {
	}

	public record AdminReportEventResponse(
			long id,
			long reportId,
			long adminUserId,
			String adminNickname,
			String eventType,
			String fromStatus,
			String toStatus,
			String memo,
			LocalDateTime createdAt
	) {
	}

	public record AdminPostHideRequest(
			String reason
	) {
	}

	public record AdminServiceNotificationRequest(
			String audience,
			Long userId,
			String title,
			String message
	) {
	}

	public record AdminServiceNotificationResponse(
			long id,
			String audience,
			Long userId,
			String title,
			String message,
			int deliveredCount,
			LocalDateTime createdAt
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
			String detailPath,
			Badge primaryBadge
	) {
		public SearchItem(String id, String type, String title, String summary, String detailPath) {
			this(id, type, title, summary, detailPath, null);
		}
	}
}
