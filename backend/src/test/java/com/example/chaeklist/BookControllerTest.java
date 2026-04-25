package com.example.chaeklist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
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
				.andExpect(jsonPath("$.todayRecommendation.id", not(emptyString())))
				.andExpect(jsonPath("$.popularBooks", hasSize(5)))
				.andExpect(jsonPath("$.trendingBooks", hasSize(5)))
				.andExpect(jsonPath("$.categoryRankings", hasSize(5)));
	}

	@Test
	void returnsRankings() throws Exception {
		mockMvc.perform(get("/api/books/rankings")
						.param("category", "전체")
						.param("period", "weekly")
						.param("limit", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].id", is("slow-reading")));
	}

	@Test
	void returnsTrendingBooks() throws Exception {
		mockMvc.perform(get("/api/books/trending")
						.param("limit", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is("attention-design")));
	}

	@Test
	void returnsCategories() throws Exception {
		mockMvc.perform(get("/api/books/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[0]", is("인문")));
	}

	@Test
	void returnsCategoryRankings() throws Exception {
		mockMvc.perform(get("/api/books/categories/{category}/rankings", "경제")
						.param("period", "weekly"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].category", is("경제")));
	}

	@Test
	void returnsBookDetail() throws Exception {
		mockMvc.perform(get("/api/books/{bookId}", "slow-reading"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is("slow-reading")))
				.andExpect(jsonPath("$.keywords", hasSize(3)));
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
	void returnsPersonalHomeWithAccessToken() throws Exception {
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personalized", is(true)))
				.andExpect(jsonPath("$.todayRecommendation.id", is("quiet-investing")));
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
