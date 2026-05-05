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

@SpringBootTest(properties = "chaeklist.reading-growth.today=2026-04-30")
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
		insertReadingPurpose(userId, "KNOWLEDGE");
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
				.andExpect(jsonPath("$.readingPurposes", hasSize(1)))
				.andExpect(jsonPath("$.readingPurposes[0].code", is("KNOWLEDGE")))
				.andExpect(jsonPath("$.readingPurposes[0].label", is("지식 확장")))
				.andExpect(jsonPath("$.readBooks", hasSize(1)))
				.andExpect(jsonPath("$.readBooks[0].id", is("901")))
				.andExpect(jsonPath("$.readBooks[0].title", is("느리게 읽는 법")))
				.andExpect(jsonPath("$.savedBooks", hasSize(1)))
				.andExpect(jsonPath("$.savedBooks[0].id", is("902")))
				.andExpect(jsonPath("$.recommendationHistory", hasSize(1)))
				.andExpect(jsonPath("$.recommendationHistory[0].title", is("느리게 읽는 법")))
				.andExpect(jsonPath("$.recommendationHistory[0].source", is("CONTENT_BASED")))
				.andExpect(jsonPath("$.readingGrowth.level", is(2)))
				.andExpect(jsonPath("$.readingGrowth.primaryBadge.code", is("FIRST_READ")))
				.andExpect(jsonPath("$.readingGrowth.monthlyReadCount", is(1)))
				.andExpect(jsonPath("$.readingGrowth.categoryDiversityCount", is(1)))
				.andExpect(jsonPath("$.readingGrowth.badges[0].code", is("FIRST_READ")));
	}

	@Test
	void returnsReadingGrowthWithPrimaryBadgeAndCalculatedMetrics() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertCategory(803, "사회");
		insertBook(904, "사회 읽기", "차민서");
		insertBookCategory(904, 803);
		insertInteraction(userId, 902, "READ", "2026-04-22 10:00:00");
		insertInteraction(userId, 904, "READ", "2026-04-23 10:00:00");
		insertRecommendation(userId, 902, "2026-04-20 09:00:00");

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readingGrowth.level", is(4)))
				.andExpect(jsonPath("$.readingGrowth.primaryBadge.code", is("PURPOSE_MATCH")))
				.andExpect(jsonPath("$.readingGrowth.monthlyReadCount", is(3)))
				.andExpect(jsonPath("$.readingGrowth.savedToReadCount", is(1)))
				.andExpect(jsonPath("$.readingGrowth.categoryDiversityCount", is(3)))
				.andExpect(jsonPath("$.readingGrowth.recommendationConversionCount", is(1)))
				.andExpect(jsonPath("$.readingGrowth.badges[?(@.code == 'CATEGORY_EXPLORER')]", hasSize(1)))
				.andExpect(jsonPath("$.readingGrowth.badges[?(@.code == 'SAVED_TO_READ')]", hasSize(1)))
				.andExpect(jsonPath("$.readingGrowth.badges[?(@.code == 'RECOMMENDATION_FOLLOWER')]", hasSize(1)))
				.andExpect(jsonPath("$.readingGrowth.badges[?(@.code == 'PURPOSE_MATCH')]", hasSize(1)));
	}

	@Test
	void returnsPrimaryReadingGrowthBadgeForHeader() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertCategory(803, "사회");
		insertBook(904, "사회 읽기", "차민서");
		insertBookCategory(904, 803);
		insertInteraction(userId, 902, "READ", "2026-04-22 10:00:00");
		insertInteraction(userId, 904, "READ", "2026-04-23 10:00:00");
		insertRecommendation(userId, 902, "2026-04-20 09:00:00");

		mockMvc.perform(get("/api/me/reading-growth/primary-badge")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code", is("PURPOSE_MATCH")))
				.andExpect(jsonPath("$.label", is("목적 맞춤 독서")))
				.andExpect(jsonPath("$.description", is("선택한 독서 목적과 맞는 책을 3권 이상 읽었습니다.")));
	}

	@Test
	void includesCompletedReadingRoomBadgeInReadingGrowth() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertCompletedReadingRoom(userId, 901, "2026-04-24 10:00:00");

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readingGrowth.badges[?(@.code == 'FIRST_READING_ROOM')]", hasSize(1)))
				.andExpect(jsonPath("$.readingGrowth.summary", is("모각독 인증을 통해 읽기 흐름을 이어가고 있습니다.")));
	}

	@Test
	void countsPurposeMatchedReadBooksByKeyword() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertCategory(804, "자기계발");
		insertKeyword(805, "AI");
		insertBook(905, "AI 습관 읽기", "오지민");
		insertBook(906, "AI 루틴 노트", "배서준");
		insertBookCategory(905, 804);
		insertBookCategory(906, 804);
		insertBookKeyword(905, 805);
		insertBookKeyword(906, 805);
		insertInteraction(userId, 905, "READ", "2026-04-24 10:00:00");
		insertInteraction(userId, 906, "READ", "2026-04-24 11:00:00");

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readingGrowth.primaryBadge.code", is("PURPOSE_MATCH")))
				.andExpect(jsonPath("$.readingGrowth.badges[?(@.code == 'PURPOSE_MATCH')]", hasSize(1)));
	}

	@Test
	void reflectsSocialActivitySlightlyInReadingGrowthScore() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		clearReadingGrowthInputs(userId);
		for (int index = 0; index < 6; index++) {
			long postId = insertSocialPost(userId, "SNS 성장 활동 " + index, "2026-04-10 10:00:00");
			insertSocialLike(postId, userId, "2026-04-10 11:00:00");
		}

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readingGrowth.level", is(1)))
				.andExpect(jsonPath("$.readingGrowth.progressPercent", is(50)));
	}

	@Test
	void countsSocialActivityOnlyForCurrentMonthInReadingGrowthScore() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		clearReadingGrowthInputs(userId);
		long previousMonthPostId = insertSocialPost(userId, "지난달 SNS 활동", "2026-03-31 10:00:00");
		insertSocialLike(previousMonthPostId, userId, "2026-03-31 11:00:00");
		long currentMonthPostId = insertSocialPost(userId, "이번달 SNS 활동", "2026-04-01 10:00:00");
		insertSocialLike(currentMonthPostId, userId, "2026-04-01 11:00:00");

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readingGrowth.level", is(1)))
				.andExpect(jsonPath("$.readingGrowth.progressPercent", is(10)));
	}

	@Test
	void ignoresDeletedAndHiddenSocialActivityInReadingGrowthScore() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		clearReadingGrowthInputs(userId);
		long hiddenPostId = insertSocialPost(userId, "숨김 SNS 활동", "2026-04-10 10:00:00");
		hideSocialPost(hiddenPostId);
		insertDeletedSocialPost(userId, "삭제 SNS 활동", "2026-04-10 10:00:00");

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readingGrowth.level", is(1)))
				.andExpect(jsonPath("$.readingGrowth.progressPercent", is(0)));
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
				.andExpect(jsonPath("$.books[2].id", is("902")))
				.andExpect(jsonPath("$.readingPurposes", hasSize(5)))
				.andExpect(jsonPath("$.readingPurposes[0].code", is("KNOWLEDGE")))
				.andExpect(jsonPath("$.readingPurposes[0].label", is("지식 확장")));
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
								  "readBookIds": [902],
								  "readingPurposeCodes": ["ECONOMY_INVESTING", "TREND_TRACKING"]
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
				.andExpect(jsonPath("$.readingPurposes", hasSize(2)))
				.andExpect(jsonPath("$.readingPurposes[0].code", is("ECONOMY_INVESTING")))
				.andExpect(jsonPath("$.readingPurposes[1].code", is("TREND_TRACKING")))
				.andExpect(jsonPath("$.readBooks", hasSize(1)))
				.andExpect(jsonPath("$.readBooks[0].id", is("902")));
	}

	@Test
	void rejectsUnsupportedReadingPurposeCode() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(put("/api/me/onboarding")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "categoryIds": [801],
								  "readBookIds": [902],
								  "readingPurposeCodes": ["UNKNOWN"]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Unsupported reading purpose code.")));
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

	@Test
	void savesAndUnsavesBookInteraction() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertBook(904, "저장 행동 테스트 책", "윤지후");
		insertBookCategory(904, 801);

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "SAVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookId", is("904")))
				.andExpect(jsonPath("$.saved", is(true)))
				.andExpect(jsonPath("$.read", is(false)))
				.andExpect(jsonPath("$.dismissed", is(false)));

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "SAVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saved", is(true)));

		org.assertj.core.api.Assertions.assertThat(countInteractions(userId, 904, "SAVE")).isEqualTo(1);

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedBooks[?(@.id == '904')]", hasSize(1)));

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "UNSAVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookId", is("904")))
				.andExpect(jsonPath("$.saved", is(false)))
				.andExpect(jsonPath("$.read", is(false)))
				.andExpect(jsonPath("$.dismissed", is(false)));

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedBooks[?(@.id == '904')]", hasSize(0)));
	}

	@Test
	void marksBookAsReadWithoutDuplicateRows() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertBook(904, "읽음 행동 테스트 책", "윤지후");
		insertBookCategory(904, 801);

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "READ"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookId", is("904")))
				.andExpect(jsonPath("$.saved", is(false)))
				.andExpect(jsonPath("$.read", is(true)))
				.andExpect(jsonPath("$.dismissed", is(false)));

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "READ"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.read", is(true)));

		org.assertj.core.api.Assertions.assertThat(countInteractions(userId, 904, "READ")).isEqualTo(1);

		mockMvc.perform(get("/api/me/mypage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readBooks[?(@.id == '904')]", hasSize(1)));
	}

	@Test
	void dismissesBookWithoutDuplicateRows() throws Exception {
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertBook(904, "관심 없음 테스트 책", "윤지후");
		insertBookCategory(904, 801);

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "DISMISS"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookId", is("904")))
				.andExpect(jsonPath("$.saved", is(false)))
				.andExpect(jsonPath("$.read", is(false)))
				.andExpect(jsonPath("$.dismissed", is(true)));

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "904")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "DISMISS"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dismissed", is(true)));

		org.assertj.core.api.Assertions.assertThat(countInteractions(userId, 904, "DISMISS")).isEqualTo(1);
	}

	@Test
	void rejectsUnsupportedBookInteractionType() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "903")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "IGNORE"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Unsupported interaction type.")));
	}

	@Test
	void rejectsBookInteractionWithoutBearerToken() throws Exception {
		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "903")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "SAVE"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message", is("Bearer token is required.")));
	}

	@Test
	void rejectsBookInteractionForMissingBook() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(post("/api/me/books/{bookId}/interactions", "999999")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type": "SAVE"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("Book not found.")));
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
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_recommendations_user_book_type UNIQUE (user_id, book_id, recommendation_type)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_reading_purposes (
					user_id BIGINT NOT NULL,
					purpose_code VARCHAR(50) NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					PRIMARY KEY (user_id, purpose_code)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS social_posts (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					user_id BIGINT,
					author_snapshot_nickname VARCHAR(50),
					author_anonymized BOOLEAN NOT NULL DEFAULT FALSE,
					post_type VARCHAR(50) NOT NULL,
					visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
					status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
					book_id BIGINT,
					recommendation_id BIGINT,
					source_interaction_id BIGINT,
					content VARCHAR(1000),
					idempotency_key VARCHAR(100),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS social_post_likes (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					post_id BIGINT NOT NULL,
					user_id BIGINT NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_social_post_likes_post_user UNIQUE (post_id, user_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS social_admin_hidden_posts (
					post_id BIGINT PRIMARY KEY,
					hidden_by_user_id BIGINT,
					reason VARCHAR(255),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
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
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
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
					canceled_at DATETIME(6)
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

	private void insertKeyword(long id, String name) {
		jdbcTemplate.update("""
				INSERT INTO keywords (id, name, keyword_type, created_at)
				VALUES (?, ?, 'TREND', CURRENT_TIMESTAMP)
				""", id, name);
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

	private void insertBookKeyword(long bookId, long keywordId) {
		jdbcTemplate.update("""
				INSERT INTO book_keywords (book_id, keyword_id)
				VALUES (?, ?)
				""", bookId, keywordId);
	}

	private void insertInterest(long userId, long categoryId) {
		jdbcTemplate.update("""
				INSERT INTO user_interest_categories (user_id, category_id, created_at)
				VALUES (?, ?, CURRENT_TIMESTAMP)
				""", userId, categoryId);
	}

	private void insertReadingPurpose(long userId, String purposeCode) {
		jdbcTemplate.update("""
				INSERT INTO user_reading_purposes (user_id, purpose_code, created_at)
				VALUES (?, ?, CURRENT_TIMESTAMP)
				""", userId, purposeCode);
	}

	private void insertInteraction(long userId, long bookId, String interactionType, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO user_book_interactions (user_id, book_id, interaction_type, created_at)
				VALUES (?, ?, ?, ?)
				""", userId, bookId, interactionType, createdAt);
	}

	private void insertCompletedReadingRoom(long userId, long bookId, String completedAt) {
		jdbcTemplate.update("""
				INSERT INTO reading_rooms (
					host_user_id, book_id, title, max_participants, status, visibility, created_at, updated_at
				)
				VALUES (?, ?, '완료된 모각독', 5, 'RECRUITING', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId, bookId);
		long roomId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_rooms", Long.class);
		jdbcTemplate.update("""
				INSERT INTO reading_room_schedules (room_id, day_of_week, day_label, scheduled_time, duration_minutes, created_at)
				VALUES (?, 6, '금요일', TIME '08:00:00', 60, CURRENT_TIMESTAMP)
				""", roomId);
		long scheduleId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_room_schedules", Long.class);
		jdbcTemplate.update("""
				INSERT INTO reading_room_sessions (
					room_id, schedule_id, session_date, scheduled_start_at, scheduled_end_at, started_at, ended_at, status, created_at, updated_at
				)
				VALUES (?, ?, DATE '2026-04-24', '2026-04-24 08:00:00', '2026-04-24 09:00:00',
					'2026-04-24 08:00:00', '2026-04-24 09:00:00', 'ENDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", roomId, scheduleId);
		long sessionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_room_sessions", Long.class);
		jdbcTemplate.update("""
				INSERT INTO reading_room_participants (room_id, user_id, status, joined_at)
				VALUES (?, ?, 'JOINED', '2026-04-24 08:00:00')
				""", roomId, userId);
		jdbcTemplate.update("""
				INSERT INTO reading_room_checkins (session_id, room_id, user_id, note, created_at)
				VALUES (?, ?, ?, '완료 인증', ?)
				""", sessionId, roomId, userId, completedAt);
	}

	private void insertRecommendation(long userId, long bookId) {
		insertRecommendation(userId, bookId, "2026-04-25 09:00:00");
	}

	private void insertRecommendation(long userId, long bookId, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO recommendations (user_id, book_id, recommendation_type, reason, score, created_at)
				VALUES (?, ?, 'CONTENT_BASED', '인문 관심 분야와 읽은 책 기록을 바탕으로 추천했습니다.', 0.9200, ?)
				""", userId, bookId, createdAt);
	}

	private void clearReadingGrowthInputs(long userId) {
		jdbcTemplate.update("DELETE FROM user_book_interactions WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM recommendations WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM social_post_likes WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM social_admin_hidden_posts");
		jdbcTemplate.update("DELETE FROM social_posts WHERE user_id = ?", userId);
	}

	private long insertSocialPost(long userId, String content, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO social_posts (
					user_id, author_snapshot_nickname, author_anonymized, post_type,
					visibility, status, content, created_at, updated_at
				)
				VALUES (?, '책리더', FALSE, 'TEXT', 'PUBLIC', 'ACTIVE', ?, ?, ?)
				""", userId, content, createdAt, createdAt);
		return jdbcTemplate.queryForObject("SELECT MAX(id) FROM social_posts", Long.class);
	}

	private void insertDeletedSocialPost(long userId, String content, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO social_posts (
					user_id, author_snapshot_nickname, author_anonymized, post_type,
					visibility, status, content, created_at, updated_at
				)
				VALUES (?, '책리더', FALSE, 'TEXT', 'PUBLIC', 'DELETED', ?, ?, ?)
				""", userId, content, createdAt, createdAt);
	}

	private void insertSocialLike(long postId, long userId, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO social_post_likes (post_id, user_id, created_at)
				VALUES (?, ?, ?)
				""", postId, userId, createdAt);
	}

	private void hideSocialPost(long postId) {
		jdbcTemplate.update("""
				INSERT INTO social_admin_hidden_posts (post_id, reason, created_at)
				VALUES (?, 'test hidden', CURRENT_TIMESTAMP)
				""", postId);
	}

	private int countInteractions(long userId, long bookId, String interactionType) {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions
				WHERE user_id = ?
					AND book_id = ?
					AND interaction_type = ?
				""", Integer.class, userId, bookId, interactionType);
	}
}
