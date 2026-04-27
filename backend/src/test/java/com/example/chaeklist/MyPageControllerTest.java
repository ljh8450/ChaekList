package com.example.chaeklist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
@Transactional
class MyPageControllerTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		createMyPageTables();
		long userId = userId();
		resetOnboardingStatus(userId);

		insertCategory(801, "인문");
		insertCategory(802, "경제");
		insertBook(901, "느리게 읽는 법", "문서윤");
		insertBook(902, "조용한 투자 습관", "서도현");
		insertBook(903, "겹치는 분야의 책", "한다겸");
		insertBookCategory(901, 801);
		insertBookCategory(902, 802);
		insertBookCategory(903, 801);
		insertBookCategory(903, 802);
		insertInterest(userId, 801);
		insertInterest(userId, 802);
		insertInteraction(userId, 901, "READ", "2026-04-20 10:00:00");
		insertInteraction(userId, 901, "VIEW", "2026-04-20 10:01:00");
		insertInteraction(userId, 902, "SAVE", "2026-04-21 10:00:00");
		insertRecommendation(userId, 901);
	}

	@Test
	void returnsMyPageDataFromDatabase() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email", is("reader@chaeklist.kr")))
				.andExpect(jsonPath("$.interests", hasSize(2)))
				.andExpect(jsonPath("$.interests[0].id", is(801)))
				.andExpect(jsonPath("$.interests[0].label", is("인문")))
				.andExpect(jsonPath("$.readBooks", hasSize(1)))
				.andExpect(jsonPath("$.readBooks[0].id", is("901")))
				.andExpect(jsonPath("$.readBooks[0].title", is("느리게 읽는 법")))
				.andExpect(jsonPath("$.savedBooks", hasSize(1)))
				.andExpect(jsonPath("$.savedBooks[0].id", is("902")))
				.andExpect(jsonPath("$.recommendationHistory", hasSize(1)))
				.andExpect(jsonPath("$.recommendationHistory[0].title", is("느리게 읽는 법")))
				.andExpect(jsonPath("$.recommendationHistory[0].source", is("CONTENT_BASED")));
	}

	@Test
	void rejectsMyPageWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/me/mypage"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message", is("Bearer token is required.")));
	}

	@Test
	void returnsOnboardingStatusFromUsersColumn() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(get("/api/me/onboarding-status")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completed", is(false)));
	}

	@Test
	void returnsOnboardingOptions() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(get("/api/me/onboarding-options")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categories", hasSize(2)))
				.andExpect(jsonPath("$.categories[0].id", is(801)))
				.andExpect(jsonPath("$.categories[0].name", is("인문")))
				.andExpect(jsonPath("$.books", hasSize(3)))
				.andExpect(jsonPath("$.books[0].id", is("903")))
				.andExpect(jsonPath("$.books[0].title", is("겹치는 분야의 책")))
				.andExpect(jsonPath("$.books[0].category", is("인문")))
				.andExpect(jsonPath("$.books[1].id", is("901")))
				.andExpect(jsonPath("$.books[2].id", is("902")));
	}

	@Test
	void savesOnboardingByReplacingPreferencesAndReflectsInMyPage() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(put("/api/me/onboarding")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "categoryIds": [801],
								  "readBookIds": [902]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completed", is(true)));

		mockMvc.perform(get("/api/me/onboarding-status")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completed", is(true)));

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.interests", hasSize(1)))
				.andExpect(jsonPath("$.interests[0].id", is(801)))
				.andExpect(jsonPath("$.interests[0].label", is("인문")))
				.andExpect(jsonPath("$.readBooks", hasSize(1)))
				.andExpect(jsonPath("$.readBooks[0].id", is("902")));
	}

	@Test
	void rejectsOnboardingWithUnsupportedIds() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(put("/api/me/onboarding")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "categoryIds": [999999],
								  "readBookIds": [902]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Unsupported category id.")));
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

	private void createMyPageTables() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_interest_categories (
					user_id BIGINT NOT NULL,
					category_id BIGINT NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					PRIMARY KEY (user_id, category_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_book_interactions (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					user_id BIGINT NOT NULL,
					book_id BIGINT NOT NULL,
					interaction_type VARCHAR(30) NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS recommendations (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					user_id BIGINT NOT NULL,
					book_id BIGINT NOT NULL,
					recommendation_type VARCHAR(30) NOT NULL,
					reason VARCHAR(255),
					score DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
					generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
	}

	private long userId() {
		return jdbcTemplate.queryForObject(
				"SELECT id FROM users WHERE email = ?",
				Long.class,
				"reader@chaeklist.kr"
		);
	}

	private void resetOnboardingStatus(long userId) {
		jdbcTemplate.update("""
				UPDATE users
				SET onboarding_completed = FALSE
				WHERE id = ?
				""", userId);
	}

	private void insertCategory(long id, String name) {
		jdbcTemplate.update("""
				INSERT INTO categories (id, name, slug, display_order, is_active, created_at, updated_at)
				VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, name, "mypage-category-" + id, (int) id);
	}

	private void insertBook(long id, String title, String author) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, ?, '마이페이지 테스트 도서', TRUE, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title, author);
	}

	private void insertBookCategory(long bookId, long categoryId) {
		jdbcTemplate.update("""
				INSERT INTO book_categories (book_id, category_id)
				VALUES (?, ?)
				""", bookId, categoryId);
	}

	private void insertInterest(long userId, long categoryId) {
		jdbcTemplate.update("""
				INSERT INTO user_interest_categories (user_id, category_id, created_at)
				VALUES (?, ?, CURRENT_TIMESTAMP)
				""", userId, categoryId);
	}

	private void insertInteraction(long userId, long bookId, String interactionType, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO user_book_interactions (user_id, book_id, interaction_type, created_at)
				VALUES (?, ?, ?, ?)
				""", userId, bookId, interactionType, createdAt);
	}

	private void insertRecommendation(long userId, long bookId) {
		jdbcTemplate.update("""
				INSERT INTO recommendations (user_id, book_id, recommendation_type, reason, score, generated_at)
				VALUES (?, ?, 'CONTENT_BASED', '인문 관심 분야와 읽은 책 기록을 바탕으로 추천했습니다.', 0.9200, CURRENT_TIMESTAMP)
				""", userId, bookId);
	}
}
