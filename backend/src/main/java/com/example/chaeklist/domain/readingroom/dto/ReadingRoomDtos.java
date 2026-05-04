package com.example.chaeklist.domain.readingroom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public final class ReadingRoomDtos {

	private ReadingRoomDtos() {
	}

	public record ReadingRoomCreateRequest(
			@Schema(description = "책 ID", example = "1")
			Long bookId,
			@Schema(description = "방 제목", example = "돈의 심리학 함께 읽기")
			String title,
			@Schema(description = "방 설명", example = "각자 조용히 읽고 끝나면 한 줄 인증합니다.")
			String description,
			@Schema(description = "시작 시간", example = "2026-05-05T21:00:00")
			LocalDateTime startAt,
			@Schema(description = "종료 시간", example = "2026-05-05T22:00:00")
			LocalDateTime endAt,
			@Schema(description = "최대 참여 인원", example = "8")
			Integer maxParticipants,
			@Schema(description = "중복 생성 방지 키", example = "room-20260505-money")
			String idempotencyKey
	) {
	}

	public record ReadingRoomCheckInRequest(
			@Schema(description = "한 줄 인증", example = "3장을 읽고 핵심 문장 하나를 정리했습니다.")
			String note,
			@Schema(description = "읽은 분량", example = "45쪽")
			String progress
	) {
	}

	public record ReadingRoomResponse(
			long id,
			long hostUserId,
			String hostNickname,
			BookSummary book,
			String title,
			String description,
			LocalDateTime startAt,
			LocalDateTime endAt,
			int maxParticipants,
			int participantCount,
			String status,
			String myParticipationStatus,
			boolean mine,
			boolean canJoin,
			boolean canCancel,
			boolean canCheckIn,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record ReadingRoomParticipantResponse(
			long roomId,
			String participationStatus,
			ReadingRoomResponse room
	) {
	}

	public record ReadingRoomCheckInResponse(
			long id,
			long roomId,
			long userId,
			String note,
			String progress,
			LocalDateTime createdAt,
			ReadingRoomResponse room
	) {
	}

	public record BookSummary(
			String id,
			String title,
			String author,
			String imageUrl,
			String category
	) {
	}
}
