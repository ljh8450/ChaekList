package com.example.chaeklist.domain.readingroom.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	private static final int MAX_DAILY_ROOMS_PER_USER = 3;
	private static final int MAX_NOTE_LENGTH = 300;
	private static final Duration MIN_DURATION = Duration.ofMinutes(20);
	private static final Duration MAX_DURATION = Duration.ofHours(4);
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
						AND host.status = 'ACTIVE'
						AND NOT EXISTS (
							SELECT 1
							FROM reading_room_admin_hidden hidden
							WHERE hidden.room_id = rr.id
						)
					ORDER BY rr.start_at ASC, rr.id ASC
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
					AND host.status = 'ACTIVE'
					AND rr.book_id = ?
					AND NOT EXISTS (
						SELECT 1
						FROM reading_room_admin_hidden hidden
						WHERE hidden.room_id = rr.id
					)
				ORDER BY rr.start_at ASC, rr.id ASC
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
				ORDER BY rr.start_at DESC, rr.id DESC
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
		LocalDateTime startAt = request == null ? null : request.startAt();
		LocalDateTime endAt = request == null ? null : request.endAt();
		int maxParticipants = normalizeMaxParticipants(request == null ? null : request.maxParticipants());
		String idempotencyKey = normalizeOptionalText(request == null ? null : request.idempotencyKey(), 100);

		validateBook(bookId);
		validateTimeRange(startAt, endAt);
		validateDailyRoomLimit(user.id(), startAt.toLocalDate());

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
							host_user_id, book_id, title, description, start_at, end_at,
							max_participants, status, visibility, idempotency_key, created_at, updated_at
						)
						VALUES (?, ?, ?, ?, ?, ?, ?, 'RECRUITING', 'PUBLIC', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
						""", Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, user.id());
				statement.setLong(2, bookId);
				statement.setString(3, title);
				statement.setString(4, description);
				statement.setTimestamp(5, Timestamp.valueOf(startAt));
				statement.setTimestamp(6, Timestamp.valueOf(endAt));
				statement.setInt(7, maxParticipants);
				statement.setString(8, idempotencyKey);
				return statement;
			}, keyHolder);
			Number key = generatedId(keyHolder);
			if (key == null) {
				throw new ReadingRoomRequestException("Reading room creation failed.");
			}
			long roomId = key.longValue();
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
		if ("IN_PROGRESS".equals(room.status()) || "ENDED".equals(room.status())) {
			throw new ReadingRoomRequestException("Participation cannot be canceled after the room has started.");
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
	public ReadingRoomCheckInResponse checkIn(AuthenticatedUser user, long roomId, ReadingRoomCheckInRequest request) {
		ReadingRoomResponse room = getReadingRoom(user, roomId);
		if (!"ENDED".equals(room.status())) {
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
						INSERT INTO reading_room_checkins (room_id, user_id, note, progress, created_at)
						VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6))
						""", Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, roomId);
				statement.setLong(2, user.id());
				statement.setString(3, note);
				statement.setString(4, progress);
				return statement;
			}, keyHolder);
			jdbcTemplate.update("""
					UPDATE reading_room_participants
					SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP(6)
					WHERE room_id = ?
						AND user_id = ?
						AND status = 'JOINED'
					""", roomId, user.id());
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
		if ("JOINED".equals(room.myParticipationStatus()) || "COMPLETED".equals(room.myParticipationStatus())) {
			return;
		}
		if (!"RECRUITING".equals(room.status())) {
			throw new ReadingRoomRequestException("Only recruiting rooms can be joined.");
		}
		if (room.participantCount() >= room.maxParticipants()) {
			throw new ReadingRoomRequestException("Reading room is full.");
		}
		validateNoOverlappingParticipation(user.id(), room.startAt(), room.endAt(), roomId);
		try {
			jdbcTemplate.update("""
					INSERT INTO reading_room_participants (room_id, user_id, status, joined_at)
					VALUES (?, ?, 'JOINED', CURRENT_TIMESTAMP(6))
					""", roomId, user.id());
		} catch (DuplicateKeyException exception) {
			jdbcTemplate.update("""
					UPDATE reading_room_participants
					SET status = 'JOINED', joined_at = CURRENT_TIMESTAMP(6), canceled_at = NULL
					WHERE room_id = ?
						AND user_id = ?
						AND status = 'CANCELED'
					""", roomId, user.id());
		}
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
		LocalDateTime startAt = resultSet.getTimestamp("start_at").toLocalDateTime();
		LocalDateTime endAt = resultSet.getTimestamp("end_at").toLocalDateTime();
		String storedStatus = resultSet.getString("status");
		String displayStatus = displayStatus(storedStatus, startAt, endAt);
		String myParticipationStatus = findMyParticipationStatus(roomId, nullableUserId(user));
		boolean mine = user != null && user.id() == hostUserId;
		boolean joined = "JOINED".equals(myParticipationStatus);
		boolean completed = "COMPLETED".equals(myParticipationStatus);
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
				startAt,
				endAt,
				maxParticipants,
				participantCount,
				displayStatus,
				myParticipationStatus,
				mine,
				user != null && "RECRUITING".equals(displayStatus) && !joined && !completed && participantCount < maxParticipants,
				user != null && joined && "RECRUITING".equals(displayStatus),
				user != null && joined && "ENDED".equals(displayStatus),
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

	private String displayStatus(String storedStatus, LocalDateTime startAt, LocalDateTime endAt) {
		if ("CANCELED".equals(storedStatus)) {
			return "CANCELED";
		}
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(startAt)) {
			return "RECRUITING";
		}
		if (now.isBefore(endAt)) {
			return "IN_PROGRESS";
		}
		return "ENDED";
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

	private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
		if (startAt == null || endAt == null) {
			throw new ReadingRoomRequestException("Start and end time are required.");
		}
		if (!startAt.isAfter(LocalDateTime.now())) {
			throw new ReadingRoomRequestException("Start time must be in the future.");
		}
		if (!endAt.isAfter(startAt)) {
			throw new ReadingRoomRequestException("End time must be after start time.");
		}
		Duration duration = Duration.between(startAt, endAt);
		if (duration.compareTo(MIN_DURATION) < 0) {
			throw new ReadingRoomRequestException("Reading room must be at least 20 minutes.");
		}
		if (duration.compareTo(MAX_DURATION) > 0) {
			throw new ReadingRoomRequestException("Reading room cannot exceed 4 hours.");
		}
	}

	private void validateDailyRoomLimit(long userId, LocalDate date) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM reading_rooms
				WHERE host_user_id = ?
					AND DATE(start_at) = ?
					AND status <> 'CANCELED'
				""", Integer.class, userId, date);
		if (count != null && count >= MAX_DAILY_ROOMS_PER_USER) {
			throw new ReadingRoomRequestException("Daily reading room creation limit exceeded.");
		}
	}

	private void validateNoOverlappingParticipation(long userId, LocalDateTime startAt, LocalDateTime endAt, long currentRoomId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM reading_room_participants participant
				JOIN reading_rooms room ON room.id = participant.room_id
				WHERE participant.user_id = ?
					AND participant.status = 'JOINED'
					AND room.status <> 'CANCELED'
					AND room.id <> ?
					AND room.start_at < ?
					AND room.end_at > ?
				""", Integer.class, userId, currentRoomId, Timestamp.valueOf(endAt), Timestamp.valueOf(startAt));
		if (count != null && count > 0) {
			throw new ReadingRoomRequestException("Overlapping reading room participation is not allowed.");
		}
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
				rr.start_at,
				rr.end_at,
				rr.max_participants,
				rr.status,
				rr.created_at,
				rr.updated_at,
				(
					SELECT COUNT(*)
					FROM reading_room_participants participant_count
					WHERE participant_count.room_id = rr.id
						AND participant_count.status IN ('JOINED', 'COMPLETED')
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
}
