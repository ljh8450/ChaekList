package com.example.chaeklist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class ReadingRoomControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@Transactional
	void createsRoomAndReturnsPublicList() throws Exception {
		createReadingRoomTables();
		insertBook(9101, "함께 읽을 책");
		String accessToken = loginAndExtractAccessToken();
		LocalDateTime startAt = LocalDateTime.now().plusDays(1).withNano(0);
		LocalDateTime endAt = startAt.plusHours(1);

		mockMvc.perform(post("/api/reading-rooms")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "bookId": 9101,
								  "title": "아침 모각독",
								  "description": "각자 조용히 읽고 인증합니다.",
								  "startAt": "%s",
								  "endAt": "%s",
								  "maxParticipants": 5,
								  "idempotencyKey": "morning-room"
								}
								""".formatted(startAt, endAt)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title", is("아침 모각독")))
				.andExpect(jsonPath("$.book.id", is("9101")))
				.andExpect(jsonPath("$.book.title", is("함께 읽을 책")))
				.andExpect(jsonPath("$.status", is("RECRUITING")))
				.andExpect(jsonPath("$.participantCount", is(1)))
				.andExpect(jsonPath("$.myParticipationStatus", is("JOINED")))
				.andExpect(jsonPath("$.mine", is(true)));

		mockMvc.perform(post("/api/reading-rooms")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "bookId": 9101,
								  "title": "아침 모각독",
								  "startAt": "%s",
								  "endAt": "%s",
								  "maxParticipants": 5,
								  "idempotencyKey": "morning-room"
								}
								""".formatted(startAt, endAt)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participantCount", is(1)));

		mockMvc.perform(get("/api/reading-rooms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title", is("아침 모각독")))
				.andExpect(jsonPath("$[0].myParticipationStatus").doesNotExist());

		mockMvc.perform(get("/api/books/{bookId}/reading-rooms", 9101)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].myParticipationStatus", is("JOINED")));
	}

	@Test
	@Transactional
	void joinsCancelsAndRejectsOverlappingParticipation() throws Exception {
		createReadingRoomTables();
		insertBook(9201, "첫 번째 책");
		insertBook(9202, "두 번째 책");
		String ownerToken = loginAndExtractAccessToken();
		String secondOwnerToken = signupAndExtractAccessToken("second-owner@chaeklist.kr", "second-owner");
		String joinerToken = signupAndExtractAccessToken("joiner@chaeklist.kr", "joiner");
		LocalDateTime startAt = LocalDateTime.now().plusDays(2).withNano(0);
		LocalDateTime endAt = startAt.plusHours(1);
		long firstRoomId = createRoom(ownerToken, 9201, "첫 모각독", startAt, endAt, "first-room");
		long secondRoomId = createRoom(secondOwnerToken, 9202, "겹치는 모각독", startAt.plusMinutes(10), endAt.plusMinutes(10), "second-room");
		long ownerId = userId();
		assertNotificationCount(ownerId, firstRoomId, 0);

		mockMvc.perform(post("/api/reading-rooms/{roomId}/participants", firstRoomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + joinerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participationStatus", is("JOINED")))
				.andExpect(jsonPath("$.room.participantCount", is(2)));
		assertNotificationCount(ownerId, firstRoomId, 1);

		mockMvc.perform(post("/api/reading-rooms/{roomId}/participants", secondRoomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + joinerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participationStatus", is("JOINED")));

		mockMvc.perform(delete("/api/reading-rooms/{roomId}/participants/me", firstRoomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + joinerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participationStatus", is("CANCELED")))
				.andExpect(jsonPath("$.room.participantCount", is(1)));
	}

	@Test
	@Transactional
	void rejectsCancelAfterRoomStarted() throws Exception {
		createReadingRoomTables();
		insertBook(9251, "진행 중 방의 책");
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		long roomId = insertInProgressRoom(userId, 9251);
		insertParticipant(roomId, userId);

		mockMvc.perform(delete("/api/reading-rooms/{roomId}/participants/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Room host cannot cancel participation.")));
	}

	@Test
	@Transactional
	void rejectsJoiningFullRoom() throws Exception {
		createReadingRoomTables();
		insertBook(9261, "정원 초과 테스트 책");
		String ownerToken = loginAndExtractAccessToken();
		String firstJoinerToken = signupAndExtractAccessToken("first-full@chaeklist.kr", "first-full");
		String secondJoinerToken = signupAndExtractAccessToken("second-full@chaeklist.kr", "second-full");
		LocalDateTime startAt = LocalDateTime.now().plusDays(2).withNano(0);
		long roomId = createRoom(ownerToken, 9261, "두 명만 읽는 모각독", startAt, startAt.plusHours(1), 2, "full-room");

		mockMvc.perform(post("/api/reading-rooms/{roomId}/participants", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + firstJoinerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.room.participantCount", is(2)));

		mockMvc.perform(post("/api/reading-rooms/{roomId}/participants", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + secondJoinerToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Reading room is full.")));
	}

	@Test
	@Transactional
	void checksInAfterRoomEnded() throws Exception {
		createReadingRoomTables();
		insertBook(9301, "종료된 방의 책");
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		long roomId = insertEndedRoom(userId, 9301);
		insertParticipant(roomId, userId);

		mockMvc.perform(post("/api/reading-rooms/{roomId}/checkins", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "한 장을 집중해서 읽었습니다.",
								  "progress": "30쪽"
								}
				"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.room.myParticipationStatus", is("JOINED")))
				.andExpect(jsonPath("$.room.canCheckIn", is(false)));

		mockMvc.perform(post("/api/reading-rooms/{roomId}/checkins", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "중복 인증"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Transactional
	void rejectsCheckInBeforeRoomEndedAndWithoutContent() throws Exception {
		createReadingRoomTables();
		insertBook(9351, "인증 경계 테스트 책");
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		long inProgressRoomId = insertInProgressRoom(userId, 9351);
		long endedRoomId = insertEndedRoom(userId, 9351);
		insertParticipant(inProgressRoomId, userId);
		insertParticipant(endedRoomId, userId);

		mockMvc.perform(post("/api/reading-rooms/{roomId}/checkins", inProgressRoomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "아직 종료 전 인증"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Check-in is only available after the room has ended.")));

		mockMvc.perform(post("/api/reading-rooms/{roomId}/checkins", endedRoomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": " ",
								  "progress": " "
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Check-in note or progress is required.")));
	}

	@Test
	@Transactional
	void adminHidesAndUnhidesReadingRoom() throws Exception {
		createReadingRoomTables();
		insertBook(9401, "숨김 대상 책");
		String ownerToken = loginAndExtractAccessToken();
		String adminToken = insertAdminAndExtractAccessToken("reading-admin@chaeklist.kr", "reading-admin");
		LocalDateTime startAt = LocalDateTime.now().plusDays(3).withNano(0);
		long roomId = createRoom(ownerToken, 9401, "숨김 대상 모각독", startAt, startAt.plusHours(1), "hide-room");

		mockMvc.perform(post("/api/admin/reading-rooms/{roomId}/hide", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "신고 검토 후 숨김"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is((int) roomId)));

		mockMvc.perform(get("/api/reading-rooms/{roomId}", roomId))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/admin/reading-rooms/{roomId}/hide", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is((int) roomId)));

		mockMvc.perform(get("/api/reading-rooms/{roomId}", roomId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is((int) roomId)));
	}

	private long createRoom(String accessToken, long bookId, String title, LocalDateTime startAt, LocalDateTime endAt, String idempotencyKey) throws Exception {
		return createRoom(accessToken, bookId, title, startAt, endAt, 5, idempotencyKey);
	}

	private long createRoom(String accessToken, long bookId, String title, LocalDateTime startAt, LocalDateTime endAt, int maxParticipants, String idempotencyKey) throws Exception {
		String responseBody = mockMvc.perform(post("/api/reading-rooms")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "bookId": %d,
								  "title": "%s",
								  "startAt": "%s",
								  "endAt": "%s",
								  "maxParticipants": %d,
								  "idempotencyKey": "%s"
								}
								""".formatted(bookId, title, startAt, endAt, maxParticipants, idempotencyKey)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(responseBody).get("id").asLong();
	}

	private String loginAndExtractAccessToken() throws Exception {
		String responseBody = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "reader@chaeklist.kr",
								  "password": "chaeklist123"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode response = objectMapper.readTree(responseBody);
		return response.get("accessToken").asText();
	}

	private String signupAndExtractAccessToken(String email, String nickname) throws Exception {
		String responseBody = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "nickname": "%s",
								  "password": "chaeklist123"
								}
								""".formatted(email, nickname)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode response = objectMapper.readTree(responseBody);
		return response.get("accessToken").asText();
	}

	private String insertAdminAndExtractAccessToken(String email, String nickname) throws Exception {
		jdbcTemplate.update("""
				INSERT INTO users (
					email, nickname, password_hash, status, role, onboarding_completed, created_at, updated_at
				)
				VALUES (?, ?, ?, 'ACTIVE', 'ADMIN', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", email, nickname, hashPassword("chaeklist123"));
		String responseBody = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "chaeklist123"
								}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode response = objectMapper.readTree(responseBody);
		return response.get("accessToken").asText();
	}

	private String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (byte value : bytes) {
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Password hashing is unavailable.", exception);
		}
	}

	private long userId() {
		return jdbcTemplate.queryForObject(
				"SELECT id FROM users WHERE email = ?",
				Long.class,
				"reader@chaeklist.kr"
		);
	}

	private void insertBook(long id, String title) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, '테스트 저자', '상세 설명', TRUE, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title);
	}

	private long insertEndedRoom(long userId, long bookId) {
		jdbcTemplate.update("""
				INSERT INTO reading_rooms (
					host_user_id, book_id, title, max_participants, status, visibility, created_at, updated_at
				)
				VALUES (?, ?, '종료된 모각독', 5, 'RECRUITING', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
		""", userId, bookId);
		long roomId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_rooms", Long.class);
		long scheduleId = insertSchedule(roomId);
		insertSession(roomId, scheduleId, "ENDED", "DATEADD('HOUR', -2, CURRENT_TIMESTAMP)", "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)");
		return roomId;
	}

	private long insertInProgressRoom(long userId, long bookId) {
		jdbcTemplate.update("""
				INSERT INTO reading_rooms (
					host_user_id, book_id, title, max_participants, status, visibility, created_at, updated_at
				)
				VALUES (?, ?, '진행 중 모각독', 5, 'RECRUITING', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId, bookId);
		long roomId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_rooms", Long.class);
		long scheduleId = insertSchedule(roomId);
		insertSession(roomId, scheduleId, "IN_PROGRESS", "DATEADD('MINUTE', -10, CURRENT_TIMESTAMP)", "DATEADD('MINUTE', 50, CURRENT_TIMESTAMP)");
		return roomId;
	}

	private long insertSchedule(long roomId) {
		jdbcTemplate.update("""
				INSERT INTO reading_room_schedules (room_id, day_of_week, day_label, scheduled_time, duration_minutes, created_at)
				VALUES (?, 2, '월요일', CURRENT_TIME, 60, CURRENT_TIMESTAMP)
				""", roomId);
		return jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_room_schedules", Long.class);
	}

	private void insertSession(long roomId, long scheduleId, String status, String startExpression, String endExpression) {
		jdbcTemplate.update("""
				INSERT INTO reading_room_sessions (
					room_id, schedule_id, session_date, scheduled_start_at, scheduled_end_at, started_at, ended_at, status, created_at, updated_at
				)
				VALUES (?, ?, CURRENT_DATE, %s, %s, %s, %s, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""".formatted(startExpression, endExpression, startExpression, "ENDED".equals(status) ? endExpression : "NULL"), roomId, scheduleId, status);
	}

	private void insertParticipant(long roomId, long userId) {
		jdbcTemplate.update("""
				INSERT INTO reading_room_participants (room_id, user_id, status, joined_at)
				VALUES (?, ?, 'JOINED', CURRENT_TIMESTAMP)
				""", roomId, userId);
	}

	private void createReadingRoomTables() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS reading_rooms (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					host_user_id BIGINT NOT NULL,
					book_id BIGINT NOT NULL,
					title VARCHAR(100) NOT NULL,
					description VARCHAR(500),
					max_participants INT NOT NULL,
					status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
					visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
					idempotency_key VARCHAR(100),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_reading_rooms_host_idempotency UNIQUE (host_user_id, idempotency_key)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS reading_room_schedules (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					room_id BIGINT NOT NULL,
					day_of_week TINYINT NOT NULL,
					day_label VARCHAR(10) NOT NULL,
					scheduled_time TIME NOT NULL,
					duration_minutes INT NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS reading_room_sessions (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					room_id BIGINT NOT NULL,
					schedule_id BIGINT NOT NULL,
					session_date DATE NOT NULL,
					scheduled_start_at DATETIME(6) NOT NULL,
					scheduled_end_at DATETIME(6) NOT NULL,
					started_at DATETIME(6),
					ended_at DATETIME(6),
					status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS reading_room_participants (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					room_id BIGINT NOT NULL,
					user_id BIGINT NOT NULL,
					status VARCHAR(20) NOT NULL DEFAULT 'JOINED',
					joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					canceled_at DATETIME(6),
					CONSTRAINT uk_reading_room_participants_room_user UNIQUE (room_id, user_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS reading_room_checkins (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					session_id BIGINT NOT NULL,
					room_id BIGINT NOT NULL,
					user_id BIGINT NOT NULL,
					note VARCHAR(300),
					progress VARCHAR(100),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_reading_room_checkins_session_user UNIQUE (session_id, user_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS reading_room_admin_hidden (
					room_id BIGINT PRIMARY KEY,
					hidden_by_user_id BIGINT,
					reason VARCHAR(255),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_notification_settings (
					user_id BIGINT PRIMARY KEY,
					like_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
					report_status_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
					service_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_notifications (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					user_id BIGINT NOT NULL,
					notification_type VARCHAR(30) NOT NULL,
					target_type VARCHAR(30) NOT NULL,
					target_id BIGINT NOT NULL,
					title VARCHAR(100) NOT NULL,
					message VARCHAR(255) NOT NULL,
					read_at DATETIME(6),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
	}

	private void assertNotificationCount(long userId, long roomId, int expectedCount) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_notifications
				WHERE user_id = ?
					AND notification_type = 'SERVICE'
					AND target_type = 'SERVICE'
					AND target_id = ?
				""", Integer.class, userId, roomId);
		org.assertj.core.api.Assertions.assertThat(count).isEqualTo(expectedCount);
	}
}
