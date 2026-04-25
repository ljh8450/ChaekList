package com.example.chaeklist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
<<<<<<< HEAD
import org.springframework.jdbc.core.JdbcTemplate;
=======
import org.springframework.test.context.jdbc.Sql;
>>>>>>> origin/develop
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void returnsPublicHome() throws Exception {
		mockMvc.perform(get("/api/home"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personalized", is(false)))
				.andExpect(jsonPath("$.todayRecommendation", nullValue()))
				.andExpect(jsonPath("$.popularBooks", hasSize(0)))
				.andExpect(jsonPath("$.trendingBooks", hasSize(0)))
				.andExpect(jsonPath("$.categoryRankings", hasSize(0)));
	}

	@Test
	void returnsRankings() throws Exception {
		mockMvc.perform(get("/api/books/rankings")
						.param("category", "전체")
						.param("period", "weekly")
						.param("limit", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@Sql(scripts = "/ranking-test-data.sql")
	@Sql(scripts = "/ranking-test-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void returnsLatestSnapshotRankings() throws Exception {
		mockMvc.perform(get("/api/books/rankings")
						.param("category", "전체")
						.param("period", "weekly")
						.param("limit", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is("102")))
				.andExpect(jsonPath("$[0].rankPosition", is(1)))
				.andExpect(jsonPath("$[0].rankingPeriod", is("WEEKLY")))
				.andExpect(jsonPath("$[0].rankDate", is("2026-04-20")))
				.andExpect(jsonPath("$[0].title", is("조용한 투자 습관")))
				.andExpect(jsonPath("$[0].views", is("2.4k")))
				.andExpect(jsonPath("$[0].saves", is(120)))
				.andExpect(jsonPath("$[0].growthRate", is("+15%")))
				.andExpect(jsonPath("$[1].id", is("101")));
	}

	@Test
	@Sql(scripts = "/ranking-test-data.sql")
	@Sql(scripts = "/ranking-test-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void returnsLatestCategorySnapshotRankings() throws Exception {
		mockMvc.perform(get("/api/books/categories/{category}/rankings", "경제")
						.param("period", "weekly")
						.param("limit", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is("102")))
				.andExpect(jsonPath("$[0].rankPosition", is(1)))
				.andExpect(jsonPath("$[0].rankingPeriod", is("WEEKLY")))
				.andExpect(jsonPath("$[0].rankDate", is("2026-04-20")))
				.andExpect(jsonPath("$[0].category", is("경제")));
	}

	@Test
	void returnsTrendingBooks() throws Exception {
		mockMvc.perform(get("/api/books/trending")
				.param("limit", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@Sql(scripts = "/ranking-test-data.sql")
	@Sql(scripts = "/ranking-test-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void returnsTrendingBooksByLatestSnapshotGrowthRate() throws Exception {
		mockMvc.perform(get("/api/books/trending")
						.param("limit", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is("102")))
				.andExpect(jsonPath("$[0].rankPosition", is(1)))
				.andExpect(jsonPath("$[0].rankingPeriod", is("WEEKLY")))
				.andExpect(jsonPath("$[0].rankDate", is("2026-04-20")))
				.andExpect(jsonPath("$[1].id", is("101")));
	}

	@Test
	void returnsCategories() throws Exception {
		mockMvc.perform(get("/api/books/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void rejectsUnknownCategoryRankings() throws Exception {
		mockMvc.perform(get("/api/books/categories/{category}/rankings", "경제")
						.param("period", "weekly"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Unsupported category.")));
	}

	@Test
	void returnsNotFoundForMissingBook() throws Exception {
		mockMvc.perform(get("/api/books/{bookId}", "missing-book"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("Book not found.")));
	}

	@Test
	@Transactional
	void returnsBookDetailWithRankedSimilarBooksAndRecommendationReason() throws Exception {
		insertCategory(101, "경제");
		insertKeyword(201, "투자");
		insertKeyword(202, "습관");
		insertBook(301, "기준 도서", true);
		insertBook(302, "공유 키워드 도서", true);
		insertBook(303, "일반 후보 1", true);
		insertBook(304, "일반 후보 2", true);
		insertBook(305, "일반 후보 3", true);
		insertBook(306, "필터 제외 도서", false);
		insertBookCategory(301, 101);
		insertBookCategory(302, 101);
		insertBookCategory(303, 101);
		insertBookCategory(304, 101);
		insertBookCategory(305, 101);
		insertBookCategory(306, 101);
		insertBookKeyword(301, 201);
		insertBookKeyword(302, 201);
		insertBookKeyword(303, 202);

		mockMvc.perform(get("/api/books/{bookId}", "301"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is("301")))
				.andExpect(jsonPath("$.recommendationReason", is("투자 키워드와 관련된 경제 분야 교양 도서입니다.")))
				.andExpect(jsonPath("$.keywords", hasSize(1)))
				.andExpect(jsonPath("$.keywords[0]", is("투자")))
				.andExpect(jsonPath("$.similarBooks", hasSize(3)))
				.andExpect(jsonPath("$.similarBooks[0].id", is("302")))
				.andExpect(jsonPath("$.similarBooks[?(@.id == '301')]", hasSize(0)))
				.andExpect(jsonPath("$.similarBooks[?(@.id == '306')]", hasSize(0)));
	}

	@Test
	void rejectsPersonalHomeWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/me/home"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message", is("Bearer token is required.")));
	}

	@Test
	void rejectsPersonalHomeWithInvalidBearerToken() throws Exception {
		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message", is("Invalid bearer token.")));
	}

	@Test
	void returnsPersonalHomeWithAccessToken() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(get("/api/me/home")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personalized", is(true)))
				.andExpect(jsonPath("$.todayRecommendation", nullValue()));
	}

	@Test
	void exposesSecurityRequirementForPersonalHome() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/me/home'].get.security[0].bearerAuth").exists());
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

	private void insertCategory(long id, String name) {
		jdbcTemplate.update("""
				INSERT INTO categories (id, name, slug, display_order, is_active, created_at, updated_at)
				VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, name, "category-" + id, 1);
	}

	private void insertKeyword(long id, String name) {
		jdbcTemplate.update("""
				INSERT INTO keywords (id, name, keyword_type, created_at)
				VALUES (?, ?, 'GENERAL', CURRENT_TIMESTAMP)
				""", id, name);
	}

	private void insertBook(long id, String title, boolean generalEligible) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, '테스트 저자', '상세 설명', ?, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title, generalEligible);
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
}
