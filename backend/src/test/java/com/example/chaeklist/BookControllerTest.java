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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
}
