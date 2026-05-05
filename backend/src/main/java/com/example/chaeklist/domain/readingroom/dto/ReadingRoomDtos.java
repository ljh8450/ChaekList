package com.example.chaeklist.domain.readingroom.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
			@Schema(description = "요일별 반복 일정")
			List<ReadingRoomScheduleRequest> schedules,
			@Schema(description = "스터디 날짜", example = "2026-05-05")
			LocalDate scheduledDate,
			@Schema(description = "스터디 시작 시각", example = "21:00:00")
			LocalTime scheduledTime,
			@Schema(description = "진행 시간(분)", example = "120")
			Integer durationMinutes,
			@Schema(description = "기존 시작 시간 호환 필드", example = "2026-05-05T21:00:00")
			LocalDateTime startAt,
			@Schema(description = "기존 종료 시간 호환 필드", example = "2026-05-05T22:00:00")
			LocalDateTime endAt,
			@Schema(description = "최대 참여 인원", example = "8")
			Integer maxParticipants,
			@Schema(description = "중복 생성 방지 키", example = "room-20260505-money")
			String idempotencyKey
	) {
	}

	public record ReadingRoomScheduleRequest(
			@Schema(description = "요일. 1=일요일, 2=월요일 ... 7=토요일", example = "4")
			Integer dayOfWeek,
			@Schema(description = "시작 시각", example = "21:00:00")
			LocalTime scheduledTime,
			@Schema(description = "진행 시간(분)", example = "90")
			Integer durationMinutes
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
			List<ReadingRoomScheduleResponse> schedules,
			LocalDate scheduledDate,
			String scheduledDayOfWeek,
			LocalTime scheduledTime,
			int durationMinutes,
			LocalDateTime startedAt,
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

	public record ReadingRoomScheduleResponse(
			long id,
			int dayOfWeek,
			String dayLabel,
			LocalTime scheduledTime,
			int durationMinutes
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
