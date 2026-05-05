package com.example.chaeklist.domain.readingroom.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.chaeklist.domain.auth.util.TokenService;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomCheckInRequest;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomCheckInResponse;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomCreateRequest;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomParticipantResponse;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomResponse;
import com.example.chaeklist.domain.readingroom.service.ReadingRoomService;
import com.example.chaeklist.domain.social.dto.SocialDtos.AdminPostHideRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.ReportRequest;
import com.example.chaeklist.domain.social.dto.SocialDtos.ReportResponse;
import com.example.chaeklist.domain.social.service.SocialService;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import com.example.chaeklist.global.auth.BearerTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reading Rooms", description = "비대면 모각독 방 생성, 조회, 참여, 인증 API")
public class ReadingRoomController {

	private final BearerTokenResolver bearerTokenResolver;
	private final ReadingRoomService readingRoomService;
	private final SocialService socialService;
	private final TokenService tokenService;

	public ReadingRoomController(
			BearerTokenResolver bearerTokenResolver,
			ReadingRoomService readingRoomService,
			SocialService socialService,
			TokenService tokenService
	) {
		this.bearerTokenResolver = bearerTokenResolver;
		this.readingRoomService = readingRoomService;
		this.socialService = socialService;
		this.tokenService = tokenService;
	}

	@GetMapping("/api/reading-rooms")
	@Operation(summary = "모각독 방 목록 조회", description = "공개 모각독 방 목록을 조회합니다.")
	public List<ReadingRoomResponse> readingRooms(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long bookId,
			@RequestParam(defaultValue = "20") int limit
	) {
		return readingRoomService.getReadingRooms(optionalAuthenticate(authorizationHeader).orElse(null), status, bookId, limit);
	}

	@GetMapping("/api/books/{bookId}/reading-rooms")
	@Operation(summary = "책 기준 모각독 방 조회", description = "책 상세 화면에서 노출할 진행 예정 또는 진행 중인 모각독 방을 조회합니다.")
	public List<ReadingRoomResponse> bookReadingRooms(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long bookId,
			@RequestParam(defaultValue = "20") int limit
	) {
		return readingRoomService.getBookReadingRooms(optionalAuthenticate(authorizationHeader).orElse(null), bookId, limit);
	}

	@GetMapping("/api/reading-rooms/{roomId}")
	@Operation(summary = "모각독 방 상세 조회", description = "모각독 방 상세와 현재 사용자의 참여 상태를 조회합니다.")
	public ReadingRoomResponse readingRoom(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId
	) {
		return readingRoomService.getReadingRoom(optionalAuthenticate(authorizationHeader).orElse(null), roomId);
	}

	@GetMapping("/api/me/reading-rooms")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 모각독 조회", description = "현재 사용자가 만들었거나 참여한 모각독 방을 조회합니다.")
	public List<ReadingRoomResponse> myReadingRooms(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "20") int limit
	) {
		return readingRoomService.getMyReadingRooms(authenticate(authorizationHeader), status, limit);
	}

	@PostMapping("/api/reading-rooms")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "모각독 방 생성", description = "특정 책을 기준으로 공개 온라인 모각독 방을 생성합니다.")
	@ApiResponse(responseCode = "200", description = "모각독 방 생성 성공")
	public ReadingRoomResponse createReadingRoom(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody ReadingRoomCreateRequest request
	) {
		return readingRoomService.createReadingRoom(authenticate(authorizationHeader), request);
	}

	@PostMapping("/api/reading-rooms/{roomId}/participants")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "모각독 참여", description = "모집 중인 모각독 방에 참여합니다.")
	public ReadingRoomParticipantResponse joinReadingRoom(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId
	) {
		return readingRoomService.joinReadingRoom(authenticate(authorizationHeader), roomId);
	}

	@PostMapping("/api/reading-rooms/{roomId}/start")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "모각독 시작", description = "방장이 모집 중인 모각독을 진행 중으로 전환합니다.")
	public ReadingRoomResponse startReadingRoom(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId
	) {
		return readingRoomService.startReadingRoom(authenticate(authorizationHeader), roomId);
	}

	@DeleteMapping("/api/reading-rooms/{roomId}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "모각독 취소", description = "방장이 시작 전 모각독 방을 취소합니다.")
	public ReadingRoomResponse cancelReadingRoom(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId
	) {
		return readingRoomService.cancelReadingRoom(authenticate(authorizationHeader), roomId);
	}

	@DeleteMapping("/api/reading-rooms/{roomId}/participants/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 모각독 참여 취소", description = "시작 전 모각독 방 참여를 취소합니다.")
	public ReadingRoomParticipantResponse cancelMyParticipation(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId
	) {
		return readingRoomService.cancelMyParticipation(authenticate(authorizationHeader), roomId);
	}

	@PostMapping("/api/reading-rooms/{roomId}/checkins")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "모각독 종료 후 인증", description = "방 종료 후 한 줄 인증 또는 읽은 분량을 기록합니다.")
	public ReadingRoomCheckInResponse checkIn(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId,
			@RequestBody ReadingRoomCheckInRequest request
	) {
		return readingRoomService.checkIn(authenticate(authorizationHeader), roomId, request);
	}

	@PostMapping("/api/reading-rooms/{roomId}/reports")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "모각독 방 신고", description = "공개 모각독 방 신고를 접수합니다.")
	public ReportResponse reportReadingRoom(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId,
			@RequestBody ReportRequest request
	) {
		ReportRequest normalizedRequest = new ReportRequest(
				"READING_ROOM",
				roomId,
				request == null ? null : request.reason(),
				request == null ? null : request.detail()
		);
		return socialService.report(authenticate(authorizationHeader), normalizedRequest);
	}

	@PostMapping("/api/admin/reading-rooms/{roomId}/hide")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 모각독 방 숨김", description = "공개 영역에서 모각독 방을 관리자 숨김 처리합니다.")
	public ReadingRoomResponse hideReadingRoomByAdmin(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId,
			@RequestBody AdminPostHideRequest request
	) {
		return readingRoomService.hideReadingRoomByAdmin(authenticate(authorizationHeader), roomId, request == null ? null : request.reason());
	}

	@DeleteMapping("/api/admin/reading-rooms/{roomId}/hide")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 모각독 방 숨김 해제", description = "모각독 방 관리자 숨김 상태를 해제합니다.")
	public ReadingRoomResponse unhideReadingRoomByAdmin(
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long roomId
	) {
		return readingRoomService.unhideReadingRoomByAdmin(authenticate(authorizationHeader), roomId);
	}

	private AuthenticatedUser authenticate(String authorizationHeader) {
		String token = bearerTokenResolver.resolve(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Bearer token is required."));
		return tokenService.validateAccessToken(token);
	}

	private Optional<AuthenticatedUser> optionalAuthenticate(String authorizationHeader) {
		try {
			return bearerTokenResolver.resolve(authorizationHeader)
					.map(tokenService::validateAccessToken);
		} catch (TokenService.TokenException exception) {
			return Optional.empty();
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

	@ExceptionHandler(ReadingRoomService.ReadingRoomRequestException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(ReadingRoomService.ReadingRoomRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler({ ReadingRoomService.ReadingRoomForbiddenException.class, SocialService.SocialForbiddenException.class })
	public ResponseEntity<Map<String, String>> handleForbidden(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler({ ReadingRoomService.ReadingRoomNotFoundException.class, SocialService.SocialNotFoundException.class, EmptyResultDataAccessException.class })
	public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Resource not found."));
	}

	@ExceptionHandler(SocialService.SocialRequestException.class)
	public ResponseEntity<Map<String, String>> handleSocialBadRequest(SocialService.SocialRequestException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
	}

	static class UnauthorizedException extends RuntimeException {

		UnauthorizedException(String message) {
			super(message);
		}
	}
}
