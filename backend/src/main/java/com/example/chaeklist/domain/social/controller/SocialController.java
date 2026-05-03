package com.example.chaeklist.domain.social.controller;

import java.util.List;
import java.util.Map;

import com.example.chaeklist.domain.auth.util.TokenService;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminPostHideRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminReportEventResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminReportResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminReportStatusRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminServiceNotificationRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminServiceNotificationResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.BlockResponse;
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
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostMediaContent;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostMediaResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostResponse;
import com.example.chaeklist.domain.social.dto.SocialDtos.SocialPostUpdateRequest;
import com.example.chaeklist.domain.social.service.SocialService;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import com.example.chaeklist.global.auth.BearerTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Social", description = "공개 피드, 공유 게시글, 좋아요, 공개 프로필, 설정 API")
public class SocialController {

	private final BearerTokenResolver bearerTokenResolver;
	private final SocialService socialService;
	private final TokenService tokenService;

	public SocialController(BearerTokenResolver bearerTokenResolver, SocialService socialService, TokenService tokenService) {
		this.bearerTokenResolver = bearerTokenResolver;
		this.socialService = socialService;
		this.tokenService = tokenService;
	}

	@GetMapping("/api/social/feed")
	@Operation(summary = "공개 피드 조회", description = "PUBLIC/ACTIVE 게시글만 조회합니다. 비활성화 계정과 관리자 숨김 게시글은 제외됩니다.")
	@ApiResponse(responseCode = "200", description = "공개 피드 조회 성공")
	public List<SocialPostResponse> feed(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) String type,
			@RequestParam(defaultValue = "latest") String sort,
			@RequestParam(defaultValue = "20") int limit
	) {
		return socialService.getFeed(optionalAuthenticate(authorizationHeader).orElse(null), type, sort, limit);
	}

	@PostMapping("/api/social/posts")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "공유 게시글 생성", description = "구조화된 공유 카드 또는 자유 텍스트 게시글을 생성합니다.")
	@ApiResponse(responseCode = "200", description = "게시글 생성 성공")
	@ApiResponse(responseCode = "400", description = "잘못된 게시글 요청")
	@ApiResponse(responseCode = "401", description = "Bearer token 누락, 만료 또는 검증 실패")
	public SocialPostResponse createPost(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody SocialPostRequest request
	) {
		return socialService.createPost(authenticate(authorizationHeader), request);
	}

	@GetMapping("/api/me/social/posts")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내가 작성한 글 조회", description = "현재 사용자가 작성한 활성 게시글을 최신순으로 조회합니다.")
	public List<SocialPostResponse> myPosts(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(defaultValue = "20") int limit
	) {
		return socialService.getMyPosts(authenticate(authorizationHeader), limit);
	}

	@GetMapping("/api/me/social/liked-posts")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "좋아요한 글 조회", description = "현재 사용자가 좋아요한 공개 게시글을 최신순으로 조회합니다.")
	public List<SocialPostResponse> likedPosts(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(defaultValue = "20") int limit
	) {
		return socialService.getLikedPosts(authenticate(authorizationHeader), limit);
	}

	@PatchMapping("/api/social/posts/{postId}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "공유 게시글 공개 범위 변경", description = "본인 게시글의 공개 범위를 PUBLIC 또는 PRIVATE로 변경합니다.")
	public SocialPostResponse updatePost(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId,
			@RequestBody SocialPostUpdateRequest request
	) {
		return socialService.updatePost(authenticate(authorizationHeader), postId, request);
	}

	@DeleteMapping("/api/social/posts/{postId}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "공유 게시글 삭제", description = "본인 게시글을 삭제 상태로 전환하고 feed에서 제외합니다.")
	public ResponseEntity<Void> deletePost(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId
	) {
		socialService.deletePost(authenticate(authorizationHeader), postId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(value = "/api/social/posts/{postId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "게시글 이미지 첨부", description = "작성자 본인의 활성 TEXT 게시글에 이미지 파일을 첨부합니다.")
	public SocialPostMediaResponse uploadPostMedia(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId,
			@RequestPart("file") MultipartFile file
	) {
		return socialService.uploadPostMedia(authenticate(authorizationHeader), postId, file);
	}

	@GetMapping("/api/social/posts/{postId}/media/{mediaId}")
	@Operation(summary = "게시글 이미지 조회", description = "공개 게시글 이미지는 공개 조회하고, 비공개 게시글 이미지는 작성자만 조회합니다.")
	public ResponseEntity<byte[]> postMedia(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId,
			@PathVariable long mediaId
	) {
		SocialPostMediaContent media = socialService.getPostMedia(optionalAuthenticate(authorizationHeader).orElse(null), postId, mediaId);
		String fileName = media.fileName() == null ? "media" : media.fileName().replace("\"", "");
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(media.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
				.body(media.data());
	}

	@PostMapping("/api/social/posts/{postId}/likes")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "좋아요", description = "공개 게시글에 좋아요를 추가합니다. 중복 요청은 멱등 처리됩니다.")
	public LikeResponse like(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId
	) {
		return socialService.likePost(authenticate(authorizationHeader), postId);
	}

	@DeleteMapping("/api/social/posts/{postId}/likes")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "좋아요 취소", description = "공개 게시글 좋아요를 취소합니다. 반복 요청은 멱등 처리됩니다.")
	public LikeResponse unlike(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId
	) {
		return socialService.unlikePost(authenticate(authorizationHeader), postId);
	}

	@PostMapping("/api/social/posts/{postId}/reports")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "게시글 신고", description = "공개 게시글 신고를 접수합니다.")
	public ReportResponse reportPost(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId,
			@RequestBody ReportRequest request
	) {
		ReportRequest normalizedRequest = new ReportRequest("POST", postId, request == null ? null : request.reason(), request == null ? null : request.detail());
		return socialService.report(authenticate(authorizationHeader), normalizedRequest);
	}

	@GetMapping("/api/users/{userId}/public-profile")
	@Operation(summary = "공개 프로필 조회", description = "공개 프로필이 허용된 활성 사용자만 조회합니다.")
	public PublicProfileResponse publicProfile(@PathVariable long userId) {
		return socialService.getPublicProfile(userId);
	}

	@GetMapping("/api/users/{userId}/social/posts")
	@Operation(summary = "공개 프로필 게시글 조회", description = "공개 프로필에서 노출 가능한 PUBLIC/ACTIVE 게시글만 최신순으로 조회합니다.")
	public List<SocialPostResponse> publicProfilePosts(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long userId,
			@RequestParam(required = false) String type,
			@RequestParam(defaultValue = "20") int limit
	) {
		return socialService.getPublicProfilePosts(optionalAuthenticate(authorizationHeader).orElse(null), userId, type, limit);
	}

	@PostMapping("/api/users/{userId}/reports")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "닉네임 신고", description = "부적절한 닉네임 신고를 접수합니다.")
	public ReportResponse reportUserNickname(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long userId,
			@RequestBody ReportRequest request
	) {
		ReportRequest normalizedRequest = new ReportRequest(
				"USER_NICKNAME",
				userId,
				request == null || request.reason() == null ? "INAPPROPRIATE_NICKNAME" : request.reason(),
				request == null ? null : request.detail()
		);
		return socialService.report(authenticate(authorizationHeader), normalizedRequest);
	}

	@PostMapping("/api/users/{userId}/blocks")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "사용자 차단", description = "피드 조회에서 차단한 사용자의 게시글을 제외합니다.")
	public BlockResponse blockUser(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long userId
	) {
		return socialService.block(authenticate(authorizationHeader), userId);
	}

	@DeleteMapping("/api/users/{userId}/blocks")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "사용자 차단 해제", description = "사용자 차단을 해제합니다.")
	public BlockResponse unblockUser(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long userId
	) {
		return socialService.unblock(authenticate(authorizationHeader), userId);
	}

	@GetMapping("/api/me/settings")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 설정 조회", description = "공개 설정과 알림 설정을 조회합니다.")
	public SettingsResponse settings(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		return socialService.getSettings(authenticate(authorizationHeader));
	}

	@PatchMapping("/api/me/privacy-settings")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "공개 설정 변경", description = "읽은 책, 저장한 책, 성장 카드, 배지, 관심 분야 공개 설정을 변경합니다.")
	public PrivacySettingsResponse updatePrivacySettings(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody PrivacySettingsRequest request
	) {
		return socialService.updatePrivacySettings(authenticate(authorizationHeader), request);
	}

	@PatchMapping("/api/me/notification-settings")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "알림 설정 변경", description = "좋아요, 신고 처리 상태, 서비스 알림 설정을 변경합니다.")
	public NotificationSettingsResponse updateNotificationSettings(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody NotificationSettingsRequest request
	) {
		return socialService.updateNotificationSettings(authenticate(authorizationHeader), request);
	}

	@GetMapping("/api/me/notifications")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 알림 목록 조회", description = "현재 사용자의 저장형 알림을 최신순으로 조회합니다.")
	public List<NotificationResponse> notifications(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(defaultValue = "20") int limit
	) {
		return socialService.getNotifications(authenticate(authorizationHeader), limit);
	}

	@PatchMapping("/api/me/notifications/{notificationId}/read")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "알림 읽음 처리", description = "현재 사용자의 알림을 읽음 상태로 전환합니다.")
	public NotificationResponse readNotification(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long notificationId
	) {
		return socialService.markNotificationRead(authenticate(authorizationHeader), notificationId);
	}

	@PostMapping("/api/me/withdraw")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회원 탈퇴", description = "사용자를 탈퇴 처리하고 기존 공개 게시글 작성자 정보는 익명화합니다.")
	public ResponseEntity<Void> withdraw(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		socialService.withdraw(authenticate(authorizationHeader));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/admin/social/reports")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 신고 목록 조회", description = "관리자 계정으로 접수된 신고를 최신순으로 조회합니다.")
	public List<AdminReportResponse> adminReports(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "20") int limit
	) {
		return socialService.getAdminReports(authenticate(authorizationHeader), status, limit);
	}

	@GetMapping("/api/admin/social/reports/{reportId}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 신고 상세 조회", description = "관리자 계정으로 신고 상세를 조회합니다.")
	public AdminReportResponse adminReport(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long reportId
	) {
		return socialService.getAdminReport(authenticate(authorizationHeader), reportId);
	}

	@PatchMapping("/api/admin/social/reports/{reportId}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 신고 상태 변경", description = "신고 상태를 PENDING, REVIEWED, REJECTED 중 하나로 변경합니다.")
	public AdminReportResponse updateAdminReport(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long reportId,
			@RequestBody AdminReportStatusRequest request
	) {
		return socialService.updateAdminReport(authenticate(authorizationHeader), reportId, request);
	}

	@GetMapping("/api/admin/social/reports/{reportId}/events")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 신고 처리 이력 조회", description = "신고 상태 변경, 운영자 메모, 닉네임 신고 처리 이력을 조회합니다.")
	public List<AdminReportEventResponse> adminReportEvents(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long reportId
	) {
		return socialService.getAdminReportEvents(authenticate(authorizationHeader), reportId);
	}

	@PostMapping("/api/admin/social/posts/{postId}/hide")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 게시글 숨김", description = "공개 영역에서 게시글을 관리자 숨김 처리합니다.")
	public SocialPostResponse hidePostByAdmin(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId,
			@RequestBody AdminPostHideRequest request
	) {
		return socialService.hidePostByAdmin(authenticate(authorizationHeader), postId, request);
	}

	@DeleteMapping("/api/admin/social/posts/{postId}/hide")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 게시글 숨김 해제", description = "게시글 관리자 숨김 상태를 해제합니다.")
	public SocialPostResponse unhidePostByAdmin(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long postId
	) {
		return socialService.unhidePostByAdmin(authenticate(authorizationHeader), postId);
	}

	@PostMapping("/api/admin/notifications/service")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 서비스 공지 알림 생성", description = "전체 활성 사용자 또는 특정 사용자에게 SERVICE 알림을 생성합니다.")
	public AdminServiceNotificationResponse createServiceNotification(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody AdminServiceNotificationRequest request
	) {
		return socialService.createServiceNotification(authenticate(authorizationHeader), request);
	}

	private AuthenticatedUser authenticate(String authorizationHeader) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		return tokenService.validateAccessToken(token);
	}

	private java.util.Optional<AuthenticatedUser> optionalAuthenticate(String authorizationHeader) {
		try {
			return bearerTokenResolver.resolve(authorizationHeader)
					.map(tokenService::validateAccessToken);
		} catch (TokenService.TokenException exception) {
			return java.util.Optional.empty();
		}
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(TokenService.TokenException.class)
	public ResponseEntity<Map<String, String>> handleInvalidToken(TokenService.TokenException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(SocialService.SocialRequestException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(SocialService.SocialRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(SocialService.SocialForbiddenException.class)
	public ResponseEntity<Map<String, String>> handleForbidden(SocialService.SocialForbiddenException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler({ SocialService.SocialNotFoundException.class, EmptyResultDataAccessException.class })
	public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Resource not found."));
	}

	static class UnauthorizedException extends RuntimeException {

		UnauthorizedException(String message) {
			super(message);
		}
	}
}
