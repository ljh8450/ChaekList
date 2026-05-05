package com.example.chaeklist.domain.readingroom.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.BookSummary;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomCheckInRequest;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomCheckInResponse;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomCreateRequest;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomParticipantResponse;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomResponse;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomScheduleRequest;
import com.example.chaeklist.domain.readingroom.dto.ReadingRoomDtos.ReadingRoomScheduleResponse;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingRoomService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;
	private static final int MIN_PARTICIPANTS = 2;
	private static final int MAX_PARTICIPANTS = 30;
	private static final int MAX_NOTE_LENGTH = 300;
	private static final Duration MIN_DURATION = Duration.ofMinutes(20);
	private static final Set<String> STATUSES = Set.of("RECRUITING", "IN_PROGRESS", "ENDED", "CANCELED");

	private final JdbcTemplate jdbcTemplate;

	public ReadingRoomService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ReadingRoomResponse> getReadingRooms(AuthenticatedUser user, String status, Long bookId, int limit) {
		String normalizedStatus = normalizeOptionalStatus(status);
		if (bookId == null) {
			return jdbcTemplate.query("""
					SELECT %s
					FROM reading_rooms rr
					JOIN users host ON host.id = rr.host_user_id
					JOIN books b ON b.id = rr.book_id
					LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
					WHERE rr.visibility = 'PUBLIC'
						AND rr.status <> 'CANCELED'
						AND host.status = 'ACTIVE'
						AND NOT EXISTS (
							SELECT 1
							FROM reading_room_admin_hidden hidden
							WHERE hidden.room_id = rr.id
						)
					ORDER BY rr.created_at DESC, rr.id DESC
					LIMIT ?
					""".formatted(selectColumns(), primaryCategorySubquery()),
					(resultSet, rowNumber) -> mapRoom(resultSet, user),
					normalizeLimit(limit)
			).stream()
					.filter(room -> normalizedStatus == null || normalizedStatus.equals(room.status()))
					.toList();
		}
		return jdbcTemplate.query("""
				SELECT %s
				FROM reading_rooms rr
				JOIN users host ON host.id = rr.host_user_id
				JOIN books b ON b.id = rr.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE rr.visibility = 'PUBLIC'
					AND rr.status <> 'CANCELED'
					AND host.status = 'ACTIVE'
					AND rr.book_id = ?
					AND NOT EXISTS (
						SELECT 1
						FROM reading_room_admin_hidden hidden
						WHERE hidden.room_id = rr.id
					)
				ORDER BY rr.created_at DESC, rr.id DESC
				LIMIT ?
				""".formatted(selectColumns(), primaryCategorySubquery()),
				(resultSet, rowNumber) -> mapRoom(resultSet, user),
				bookId,
				normalizeLimit(limit)
		).stream()
				.filter(room -> normalizedStatus == null || normalizedStatus.equals(room.status()))
				.toList();
	}

	public List<ReadingRoomResponse> getBookReadingRooms(AuthenticatedUser user, long bookId, int limit) {
		return getReadingRooms(user, null, bookId, limit).stream()
				.filter(room -> !"ENDED".equals(room.status()) && !"CANCELED".equals(room.status()))
				.toList();
	}

	public ReadingRoomResponse getReadingRoom(AuthenticatedUser user, long roomId) {
		return findRoom(user, roomId).orElseThrow(() -> new ReadingRoomNotFoundException("Reading room not found."));
	}

	@Transactional
	public ReadingRoomResponse hideReadingRoomByAdmin(AuthenticatedUser user, long roomId, String reason) {
		validateAdmin(user);
		getAdminReadingRoom(user, roomId);
		jdbcTemplate.update("""
				INSERT INTO reading_room_admin_hidden (room_id, hidden_by_user_id, reason, created_at)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP(6))
				ON DUPLICATE KEY UPDATE
					hidden_by_user_id = VALUES(hidden_by_user_id),
					reason = VALUES(reason),
					created_at = CURRENT_TIMESTAMP(6)
				""", roomId, user.id(), normalizeOptionalText(reason, 255));
		return getAdminReadingRoom(user, roomId);
	}

	@Transactional
	public ReadingRoomResponse unhideReadingRoomByAdmin(AuthenticatedUser user, long roomId) {
		validateAdmin(user);
		ReadingRoomResponse room = getAdminReadingRoom(user, roomId);
		jdbcTemplate.update("DELETE FROM reading_room_admin_hidden WHERE room_id = ?", roomId);
		return room;
	}

	public List<ReadingRoomResponse> getMyReadingRooms(AuthenticatedUser user, String status, int limit) {
		String normalizedStatus = normalizeOptionalStatus(status);
		return jdbcTemplate.query("""
				SELECT %s
				FROM reading_rooms rr
				JOIN users host ON host.id = rr.host_user_id
				JOIN books b ON b.id = rr.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				LEFT JOIN reading_room_participants participant
					ON participant.room_id = rr.id
					AND participant.user_id = ?
				WHERE rr.host_user_id = ?
					OR participant.user_id = ?
				ORDER BY rr.created_at DESC, rr.id DESC
				LIMIT ?
				""".formatted(selectColumns(), primaryCategorySubquery()),
				(resultSet, rowNumber) -> mapRoom(resultSet, user),
				user.id(),
				user.id(),
				user.id(),
				normalizeLimit(limit)
		).stream()
				.filter(room -> normalizedStatus == null || normalizedStatus.equals(room.status()))
				.toList();
	}

	@Transactional
	public ReadingRoomResponse createReadingRoom(AuthenticatedUser user, ReadingRoomCreateRequest request) {
		Long bookId = request == null ? null : request.bookId();
		String title = normalizeRequiredText(request == null ? null : request.title(), "Title is required.", 100);
		String description = normalizeOptionalText(request == null ? null : request.description(), 500);
		List<NormalizedSchedule> schedules = normalizeSchedules(request);
		int maxParticipants = normalizeMaxParticipants(request == null ? null : request.maxParticipants());
		String idempotencyKey = normalizeOptionalText(request == null ? null : request.idempotencyKey(), 100);

		validateBook(bookId);

		if (idempotencyKey != null) {
			Optional<ReadingRoomResponse> existingRoom = findRoomByIdempotencyKey(user.id(), idempotencyKey);
			if (existingRoom.isPresent()) {
				return existingRoom.get();
			}
		}

		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				PreparedStatement statement = connection.prepareStatement("""
						INSERT INTO reading_rooms (
							host_user_id, book_id, title, description,
							max_participants, status, visibility, idempotency_key, created_at, updated_at
						)
						VALUES (?, ?, ?, ?, ?, 'RECRUITING', 'PUBLIC', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
						""", Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, user.id());
				statement.setLong(2, bookId);
				statement.setString(3, title);
				statement.setString(4, description);
				statement.setInt(5, maxParticipants);
				statement.setString(6, idempotencyKey);
				return statement;
			}, keyHolder);
			Number key = generatedId(keyHolder);
			if (key == null) {
				throw new ReadingRoomRequestException("Reading room creation failed.");
			}
			long roomId = key.longValue();
			insertSchedules(roomId, schedules);
			joinRoom(user, roomId);
			return getReadingRoom(user, roomId);
		} catch (DuplicateKeyException exception) {
			if (idempotencyKey == null) {
				throw exception;
			}
			return findRoomByIdempotencyKey(user.id(), idempotencyKey)
					.orElseThrow(() -> new ReadingRoomRequestException("Reading room creation conflict."));
		}
	}

	@Transactional
	public ReadingRoomParticipantResponse joinReadingRoom(AuthenticatedUser user, long roomId) {
		joinRoom(user, roomId);
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		return new ReadingRoomParticipantResponse(roomId, room.myParticipationStatus(), room);
	}

	@Transactional
	public ReadingRoomParticipantResponse cancelMyParticipation(AuthenticatedUser user, long roomId) {
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		if (room.hostUserId() == user.id()) {
			throw new ReadingRoomRequestException("Room host cannot cancel participation.");
		}
		int updated = jdbcTemplate.update("""
				UPDATE reading_room_participants
				SET status = 'CANCELED', canceled_at = CURRENT_TIMESTAMP(6)
				WHERE room_id = ?
					AND user_id = ?
					AND status = 'JOINED'
				""", roomId, user.id());
		if (updated == 0) {
			throw new ReadingRoomRequestException("Active participation not found.");
		}
		ReadingRoomResponse updatedRoom = getReadingRoom(user, roomId);
		return new ReadingRoomParticipantResponse(roomId, updatedRoom.myParticipationStatus(), updatedRoom);
	}

	@Transactional
	public ReadingRoomResponse startReadingRoom(AuthenticatedUser user, long roomId) {
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		validateRoomHost(user, room);
		createManualSession(room);
		return getReadingRoom(user, roomId);
	}

	@Transactional
	public ReadingRoomResponse cancelReadingRoom(AuthenticatedUser user, long roomId) {
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		validateRoomHost(user, room);
		jdbcTemplate.update("""
				UPDATE reading_rooms
				SET status = 'CANCELED',
					updated_at = CURRENT_TIMESTAMP(6)
				WHERE id = ?
				""", roomId);
		jdbcTemplate.update("UPDATE reading_room_sessions SET status = 'CANCELED' WHERE room_id = ? AND status <> 'ENDED'", roomId);
		return getAdminReadingRoom(user, roomId);
	}

	@Transactional
	public ReadingRoomCheckInResponse checkIn(AuthenticatedUser user, long roomId, ReadingRoomCheckInRequest request) {
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		Long sessionId = findLatestEndedSessionId(roomId);
		if (sessionId == null) {
			throw new ReadingRoomRequestException("Check-in is only available after the room has ended.");
		}
		if (!"JOINED".equals(room.myParticipationStatus())) {
			throw new ReadingRoomRequestException("Joined participation is required for check-in.");
		}
		String note = normalizeOptionalText(request == null ? null : request.note(), MAX_NOTE_LENGTH);
		String progress = normalizeOptionalText(request == null ? null : request.progress(), 100);
		if (note == null && progress == null) {
			throw new ReadingRoomRequestException("Check-in note or progress is required.");
		}
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				PreparedStatement statement = connection.prepareStatement("""
						INSERT INTO reading_room_checkins (session_id, room_id, user_id, note, progress, created_at)
						VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
						""", Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, sessionId);
				statement.setLong(2, roomId);
				statement.setLong(3, user.id());
				statement.setString(4, note);
				statement.setString(5, progress);
				return statement;
			}, keyHolder);
			Number key = generatedId(keyHolder);
			if (key == null) {
				throw new ReadingRoomRequestException("Check-in creation failed.");
			}
			ReadingRoomResponse updatedRoom = getReadingRoom(user, roomId);
			return new ReadingRoomCheckInResponse(key.longValue(), roomId, user.id(), note, progress, LocalDateTime.now(), updatedRoom);
		} catch (DuplicateKeyException exception) {
			throw new ReadingRoomRequestException("Check-in already exists.");
		}
	}

	private void joinRoom(AuthenticatedUser user, long roomId) {
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		if ("JOINED".equals(room.myParticipationStatus())) {
			return;
		}
		if (!"RECRUITING".equals(room.status())) {
			throw new ReadingRoomRequestException("Only recruiting rooms can be joined.");
		}
		if (room.participantCount() >= room.maxParticipants()) {
			throw new ReadingRoomRequestException("Reading room is full.");
		}
		try {
			jdbcTemplate.update("""
					INSERT INTO reading_room_participants (room_id, user_id, status, joined_at)
					VALUES (?, ?, 'JOINED', CURRENT_TIMESTAMP(6))
					""", roomId, user.id());
			createParticipantNotification(room, user);
		} catch (DuplicateKeyException exception) {
			int updated = jdbcTemplate.update("""
					UPDATE reading_room_participants
					SET status = 'JOINED', joined_at = CURRENT_TIMESTAMP(6), canceled_at = NULL
					WHERE room_id = ?
						AND user_id = ?
						AND status = 'CANCELED'
					""", roomId, user.id());
			if (updated > 0) {
				createParticipantNotification(room, user);
			}
		}
	}

	private void createParticipantNotification(ReadingRoomResponse room, AuthenticatedUser participant) {
		if (room.hostUserId() == participant.id() || !isServiceNotificationEnabled(room.hostUserId())) {
			return;
		}
		jdbcTemplate.update("""
				INSERT INTO user_notifications (
					user_id, notification_type, target_type, target_id, title, message, created_at
				)
				VALUES (?, 'SERVICE', 'SERVICE', ?, ?, ?, CURRENT_TIMESTAMP(6))
				""",
				room.hostUserId(),
				room.id(),
				"모각독 새 참여자 알림",
				participant.nickname() + "님이 '" + room.title() + "' 모각독에 참여했습니다."
		);
	}

	private boolean isServiceNotificationEnabled(long userId) {
		Boolean enabled = jdbcTemplate.queryForObject("""
				SELECT COALESCE((
					SELECT service_notifications_enabled
					FROM user_notification_settings
					WHERE user_id = ?
				), TRUE)
				""", Boolean.class, userId);
		return Boolean.TRUE.equals(enabled);
	}

	private Optional<ReadingRoomResponse> findRoom(AuthenticatedUser user, long roomId) {
		List<ReadingRoomResponse> rooms = jdbcTemplate.query("""
				SELECT %s
				FROM reading_rooms rr
				JOIN users host ON host.id = rr.host_user_id
				JOIN books b ON b.id = rr.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE rr.id = ?
					AND rr.visibility = 'PUBLIC'
					AND rr.status <> 'CANCELED'
					AND host.status = 'ACTIVE'
					AND NOT EXISTS (
						SELECT 1
						FROM reading_room_admin_hidden hidden
						WHERE hidden.room_id = rr.id
					)
				""".formatted(selectColumns(), primaryCategorySubquery()),
				(resultSet, rowNumber) -> mapRoom(resultSet, user),
				roomId
		);
		return rooms.stream().findFirst();
	}

	private Long findLatestEndedSessionId(long roomId) {
		List<Long> sessionIds = jdbcTemplate.query("""
				SELECT id
				FROM reading_room_sessions
				WHERE room_id = ?
					AND status = 'ENDED'
				ORDER BY scheduled_start_at DESC, id DESC
				LIMIT 1
				""", (resultSet, rowNumber) -> resultSet.getLong("id"), roomId);
		return sessionIds.stream().findFirst().orElse(null);
	}

	private ReadingRoomResponse getAdminReadingRoom(AuthenticatedUser user, long roomId) {
		List<ReadingRoomResponse> rooms = jdbcTemplate.query("""
				SELECT %s
				FROM reading_rooms rr
				JOIN users host ON host.id = rr.host_user_id
				JOIN books b ON b.id = rr.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE rr.id = ?
				""".formatted(selectColumns(), primaryCategorySubquery()),
				(resultSet, rowNumber) -> mapRoom(resultSet, user),
				roomId
		);
		return rooms.stream().findFirst().orElseThrow(() -> new ReadingRoomNotFoundException("Reading room not found."));
	}

	private void validateAdmin(AuthenticatedUser user) {
		if (user == null || !"ADMIN".equals(user.role())) {
			throw new ReadingRoomForbiddenException("Admin access is required.");
		}
	}

	private void validateRoomHost(AuthenticatedUser user, ReadingRoomResponse room) {
		if (user == null || room.hostUserId() != user.id()) {
			throw new ReadingRoomForbiddenException("Only the room host can change this reading room.");
		}
	}

	private Optional<ReadingRoomResponse> findRoomByIdempotencyKey(long userId, String idempotencyKey) {
		List<ReadingRoomResponse> rooms = jdbcTemplate.query("""
				SELECT %s
				FROM reading_rooms rr
				JOIN users host ON host.id = rr.host_user_id
				JOIN books b ON b.id = rr.book_id
				LEFT JOIN (%s) primary_category ON primary_category.book_id = b.id
				WHERE rr.host_user_id = ?
					AND rr.idempotency_key = ?
				""".formatted(selectColumns(), primaryCategorySubquery()),
				(resultSet, rowNumber) -> mapRoom(resultSet, new AuthenticatedUser(userId, "", "", "ACTIVE", "USER")),
				userId,
				idempotencyKey
		);
		return rooms.stream().findFirst();
	}

	private ReadingRoomResponse mapRoom(ResultSet resultSet, AuthenticatedUser user) throws SQLException {
		long roomId = resultSet.getLong("id");
		long hostUserId = resultSet.getLong("host_user_id");
		List<ReadingRoomScheduleResponse> schedules = findSchedules(roomId);
		ReadingRoomScheduleResponse primarySchedule = schedules.isEmpty() ? null : schedules.getFirst();
		CurrentSession currentSession = findCurrentSession(roomId);
		String storedStatus = resultSet.getString("status");
		String displayStatus = displayStatus(storedStatus, currentSession);
		String myParticipationStatus = findMyParticipationStatus(roomId, nullableUserId(user));
		boolean mine = user != null && user.id() == hostUserId;
		boolean joined = "JOINED".equals(myParticipationStatus);
		boolean completed = user != null && hasLatestSessionCheckIn(roomId, user.id());
		int participantCount = resultSet.getInt("participant_count");
		int maxParticipants = resultSet.getInt("max_participants");
		return new ReadingRoomResponse(
				roomId,
				hostUserId,
				resultSet.getString("host_nickname"),
				new BookSummary(
						String.valueOf(resultSet.getLong("book_id")),
						resultSet.getString("book_title"),
						resultSet.getString("book_author"),
						resultSet.getString("book_image_url"),
						resultSet.getString("book_category") == null ? "미분류" : resultSet.getString("book_category")
				),
				resultSet.getString("title"),
				resultSet.getString("description"),
				schedules,
				null,
				primarySchedule == null ? null : primarySchedule.dayLabel(),
				primarySchedule == null ? null : primarySchedule.scheduledTime(),
				primarySchedule == null ? 0 : primarySchedule.durationMinutes(),
				currentSession == null ? null : currentSession.startedAt(),
				currentSession == null ? null : currentSession.scheduledStartAt(),
				currentSession == null ? null : currentSession.scheduledEndAt(),
				maxParticipants,
				participantCount,
				displayStatus,
				myParticipationStatus,
				mine,
				user != null && "RECRUITING".equals(displayStatus) && !joined && !completed && participantCount < maxParticipants,
				user != null && joined && !mine && "RECRUITING".equals(displayStatus),
				user != null && joined && "ENDED".equals(displayStatus) && !completed,
				resultSet.getTimestamp("created_at").toLocalDateTime(),
				resultSet.getTimestamp("updated_at").toLocalDateTime()
		);
	}

	private String findMyParticipationStatus(long roomId, Long userId) {
		if (userId == null) {
			return null;
		}
		List<String> statuses = jdbcTemplate.query("""
				SELECT status
				FROM reading_room_participants
				WHERE room_id = ?
					AND user_id = ?
				""", (resultSet, rowNumber) -> resultSet.getString("status"), roomId, userId);
		return statuses.stream().findFirst().orElse(null);
	}

	private List<ReadingRoomScheduleResponse> findSchedules(long roomId) {
		return jdbcTemplate.query("""
				SELECT id, day_of_week, day_label, scheduled_time, duration_minutes
				FROM reading_room_schedules
				WHERE room_id = ?
				ORDER BY day_of_week ASC, scheduled_time ASC, id ASC
				""", (resultSet, rowNumber) -> new ReadingRoomScheduleResponse(
				resultSet.getLong("id"),
				resultSet.getInt("day_of_week"),
				resultSet.getString("day_label"),
				resultSet.getTime("scheduled_time").toLocalTime(),
				resultSet.getInt("duration_minutes")
		), roomId);
	}

	private CurrentSession findCurrentSession(long roomId) {
		List<CurrentSession> sessions = jdbcTemplate.query("""
				SELECT id, scheduled_start_at, scheduled_end_at, started_at, ended_at, status
				FROM reading_room_sessions
				WHERE room_id = ?
					AND status IN ('IN_PROGRESS', 'ENDED')
				ORDER BY scheduled_start_at DESC, id DESC
				LIMIT 1
				""", (resultSet, rowNumber) -> new CurrentSession(
				resultSet.getLong("id"),
				resultSet.getTimestamp("scheduled_start_at").toLocalDateTime(),
				resultSet.getTimestamp("scheduled_end_at").toLocalDateTime(),
				resultSet.getTimestamp("started_at") == null ? null : resultSet.getTimestamp("started_at").toLocalDateTime(),
				resultSet.getTimestamp("ended_at") == null ? null : resultSet.getTimestamp("ended_at").toLocalDateTime(),
				resultSet.getString("status")
		), roomId);
		return sessions.stream().findFirst().orElse(null);
	}

	private boolean hasLatestSessionCheckIn(long roomId, long userId) {
		Long sessionId = findLatestEndedSessionId(roomId);
		if (sessionId == null) {
			return false;
		}
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM reading_room_checkins
				WHERE session_id = ?
					AND user_id = ?
				""", Integer.class, sessionId, userId);
		return count != null && count > 0;
	}

	private String displayStatus(String storedStatus, CurrentSession currentSession) {
		if ("CANCELED".equals(storedStatus)) {
			return "CANCELED";
		}
		if (currentSession == null) {
			return "RECRUITING";
		}
		if ("IN_PROGRESS".equals(currentSession.status())) {
			return "IN_PROGRESS";
		}
		return "ENDED".equals(currentSession.status()) ? "ENDED" : "RECRUITING";
	}

	private void validateBook(Long bookId) {
		if (bookId == null) {
			throw new ReadingRoomRequestException("Book id is required.");
		}
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM books
				WHERE id = ?
				""", Integer.class, bookId);
		if (count == null || count == 0) {
			throw new ReadingRoomRequestException("Book not found.");
		}
	}

	private List<NormalizedSchedule> normalizeSchedules(ReadingRoomCreateRequest request) {
		List<NormalizedSchedule> schedules = new ArrayList<>();
		if (request != null && request.schedules() != null) {
			for (ReadingRoomScheduleRequest schedule : request.schedules()) {
				schedules.add(normalizeSchedule(schedule));
			}
		}
		if (schedules.isEmpty() && request != null && request.scheduledDate() != null && request.scheduledTime() != null) {
			schedules.add(new NormalizedSchedule(
					dayOfWeekValue(request.scheduledDate().getDayOfWeek()),
					scheduledDayOfWeek(request.scheduledDate().getDayOfWeek()),
					request.scheduledTime(),
					normalizeDurationMinutes(request.durationMinutes())
			));
		}
		if (schedules.isEmpty() && request != null && request.startAt() != null && request.endAt() != null) {
			schedules.add(new NormalizedSchedule(
					dayOfWeekValue(request.startAt().getDayOfWeek()),
					scheduledDayOfWeek(request.startAt().getDayOfWeek()),
					request.startAt().toLocalTime(),
					normalizeDurationMinutes(Math.toIntExact(Duration.between(request.startAt(), request.endAt()).toMinutes()))
			));
		}
		if (schedules.isEmpty()) {
			throw new ReadingRoomRequestException("Schedule is required.");
		}
		return schedules;
	}

	private NormalizedSchedule normalizeSchedule(ReadingRoomScheduleRequest schedule) {
		if (schedule == null || schedule.dayOfWeek() == null || schedule.scheduledTime() == null) {
			throw new ReadingRoomRequestException("Schedule day and time are required.");
		}
		if (schedule.dayOfWeek() < 1 || schedule.dayOfWeek() > 7) {
			throw new ReadingRoomRequestException("Schedule day must be between 1 and 7.");
		}
		return new NormalizedSchedule(
				schedule.dayOfWeek(),
				dayLabel(schedule.dayOfWeek()),
				schedule.scheduledTime(),
				normalizeDurationMinutes(schedule.durationMinutes())
		);
	}

	private int normalizeDurationMinutes(Integer durationMinutes) {
		if (durationMinutes == null) {
			throw new ReadingRoomRequestException("Duration is required.");
		}
		if (Duration.ofMinutes(durationMinutes).compareTo(MIN_DURATION) < 0) {
			throw new ReadingRoomRequestException("Reading room must be at least 20 minutes.");
		}
		return durationMinutes;
	}

	private void insertSchedules(long roomId, List<NormalizedSchedule> schedules) {
		for (NormalizedSchedule schedule : schedules) {
			jdbcTemplate.update("""
					INSERT INTO reading_room_schedules (
						room_id, day_of_week, day_label, scheduled_time, duration_minutes, created_at
					)
					VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
					""",
					roomId,
					schedule.dayOfWeek(),
					schedule.dayLabel(),
					Time.valueOf(schedule.scheduledTime()),
					schedule.durationMinutes()
			);
		}
	}

	private void createManualSession(ReadingRoomResponse room) {
		if (room.schedules().isEmpty()) {
			throw new ReadingRoomRequestException("Schedule is required.");
		}
		ReadingRoomScheduleResponse schedule = room.schedules().getFirst();
		LocalDateTime startedAt = LocalDateTime.now();
		LocalDateTime endedAt = startedAt.plusMinutes(schedule.durationMinutes());
		jdbcTemplate.update("""
				INSERT INTO reading_room_sessions (
					room_id, schedule_id, session_date, scheduled_start_at, scheduled_end_at,
					started_at, status, created_at, updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
				""",
				room.id(),
				schedule.id(),
				java.sql.Date.valueOf(startedAt.toLocalDate()),
				Timestamp.valueOf(startedAt),
				Timestamp.valueOf(endedAt),
				Timestamp.valueOf(startedAt)
		);
	}

	private int dayOfWeekValue(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case SUNDAY -> 1;
			case MONDAY -> 2;
			case TUESDAY -> 3;
			case WEDNESDAY -> 4;
			case THURSDAY -> 5;
			case FRIDAY -> 6;
			case SATURDAY -> 7;
		};
	}

	private String scheduledDayOfWeek(DayOfWeek dayOfWeek) {
		return dayLabel(dayOfWeekValue(dayOfWeek));
	}

	private String dayLabel(int dayOfWeek) {
		return switch (dayOfWeek) {
			case 1 -> "일요일";
			case 2 -> "월요일";
			case 3 -> "화요일";
			case 4 -> "수요일";
			case 5 -> "목요일";
			case 6 -> "금요일";
			case 7 -> "토요일";
			default -> throw new ReadingRoomRequestException("Schedule day must be between 1 and 7.");
		};
	}

	private String normalizeOptionalStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
		if (!STATUSES.contains(normalizedStatus)) {
			throw new ReadingRoomRequestException("Unsupported reading room status.");
		}
		return normalizedStatus;
	}

	private int normalizeMaxParticipants(Integer maxParticipants) {
		int normalized = maxParticipants == null ? MIN_PARTICIPANTS : maxParticipants;
		if (normalized < MIN_PARTICIPANTS || normalized > MAX_PARTICIPANTS) {
			throw new ReadingRoomRequestException("Max participants must be between 2 and 30.");
		}
		return normalized;
	}

	private int normalizeLimit(int limit) {
		if (limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private String normalizeRequiredText(String value, String message, int maxLength) {
		String normalized = normalizeOptionalText(value, maxLength);
		if (normalized == null) {
			throw new ReadingRoomRequestException(message);
		}
		return normalized;
	}

	private String normalizeOptionalText(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isBlank()) {
			return null;
		}
		if (normalized.length() > maxLength) {
			throw new ReadingRoomRequestException("Text is too long.");
		}
		return normalized;
	}

	private Long nullableUserId(AuthenticatedUser user) {
		return user == null ? null : user.id();
	}

	private Number generatedId(KeyHolder keyHolder) {
		if (!keyHolder.getKeyList().isEmpty() && keyHolder.getKeyList().getFirst().containsKey("id")) {
			Object id = keyHolder.getKeyList().getFirst().get("id");
			if (id instanceof Number number) {
				return number;
			}
		}
		return keyHolder.getKey();
	}

	private String selectColumns() {
		return """
				rr.id,
				rr.host_user_id,
				host.nickname AS host_nickname,
				rr.book_id,
				b.title AS book_title,
				b.author AS book_author,
				b.cover_image_url AS book_image_url,
				primary_category.name AS book_category,
				rr.title,
				rr.description,
				rr.max_participants,
				rr.status,
				rr.created_at,
				rr.updated_at,
				(
					SELECT COUNT(*)
					FROM reading_room_participants participant_count
					WHERE participant_count.room_id = rr.id
						AND participant_count.status = 'JOINED'
				) AS participant_count
				""";
	}

	private String primaryCategorySubquery() {
		return """
				SELECT bc.book_id, c.name
				FROM book_categories bc
				JOIN categories c ON c.id = bc.category_id
				WHERE bc.category_id = (
					SELECT bc_inner.category_id
					FROM book_categories bc_inner
					JOIN categories c_inner ON c_inner.id = bc_inner.category_id
					WHERE bc_inner.book_id = bc.book_id
					ORDER BY c_inner.display_order ASC, c_inner.id ASC
					LIMIT 1
				)
				""";
	}

	public static class ReadingRoomRequestException extends RuntimeException {

		public ReadingRoomRequestException(String message) {
			super(message);
		}
	}

	public static class ReadingRoomNotFoundException extends RuntimeException {

		public ReadingRoomNotFoundException(String message) {
			super(message);
		}
	}

	public static class ReadingRoomForbiddenException extends RuntimeException {

		public ReadingRoomForbiddenException(String message) {
			super(message);
		}
	}

	private record NormalizedSchedule(
			int dayOfWeek,
			String dayLabel,
			LocalTime scheduledTime,
			int durationMinutes
	) {
	}

	private record CurrentSession(
			long id,
			LocalDateTime scheduledStartAt,
			LocalDateTime scheduledEndAt,
			LocalDateTime startedAt,
			LocalDateTime endedAt,
			String status
	) {
	}
}
