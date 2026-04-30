package com.example.chaeklist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class SocialControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@Transactional
	void createsTextPostAndReturnsPublicFeed() throws Exception {
		createSocialTables();
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(post("/api/social/posts")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "postType": "TEXT",
								  "content": "오늘 읽은 책이 좋았습니다.",
								  "visibility": "PUBLIC",
								  "idempotencyKey": "text-post-1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postType", is("TEXT")))
				.andExpect(jsonPath("$.visibility", is("PUBLIC")))
				.andExpect(jsonPath("$.content", is("오늘 읽은 책이 좋았습니다.")));

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].postType", is("TEXT")))
				.andExpect(jsonPath("$[0].nickname", is("quiet-reader")));
	}

	@Test
	@Transactional
	void likesAreIdempotent() throws Exception {
		createSocialTables();
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		long postId = insertPublicTextPost(userId, "멱등 좋아요 테스트");

		mockMvc.perform(post("/api/social/posts/{postId}/likes", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked", is(true)))
				.andExpect(jsonPath("$.likeCount", is(1)));

		mockMvc.perform(post("/api/social/posts/{postId}/likes", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked", is(true)))
				.andExpect(jsonPath("$.likeCount", is(1)));

		mockMvc.perform(get("/api/social/feed")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].likedByMe", is(true)))
				.andExpect(jsonPath("$[0].mine", is(true)))
				.andExpect(jsonPath("$[0].likeCount", is(1)));

		mockMvc.perform(delete("/api/social/posts/{postId}/likes", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked", is(false)))
				.andExpect(jsonPath("$.likeCount", is(0)));

		mockMvc.perform(get("/api/social/feed")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].likedByMe", is(false)))
				.andExpect(jsonPath("$[0].likeCount", is(0)));
	}

	@Test
	@Transactional
	void hidesInactiveUserPostsFromFeedAndSearch() throws Exception {
		createSocialTables();
		long userId = userId();
		insertPublicTextPost(userId, "비활성화 게시글");
		jdbcTemplate.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", userId);

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/api/search/posts")
						.param("query", "비활성화"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@Transactional
	void searchesPublicPostsOnly() throws Exception {
		createSocialTables();
		long userId = userId();
		insertPublicTextPost(userId, "공개 투자 기록");
		insertPrivateTextPost(userId, "비공개 투자 기록");

		mockMvc.perform(get("/api/search")
						.param("query", "투자")
						.param("type", "posts")
						.param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].items", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].items[0].summary", is("공개 투자 기록")));
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

	private long userId() {
		return jdbcTemplate.queryForObject(
				"SELECT id FROM users WHERE email = ?",
				Long.class,
				"reader@chaeklist.kr"
		);
	}

	private long insertPublicTextPost(long userId, String content) {
		return insertTextPost(userId, content, "PUBLIC");
	}

	private long insertPrivateTextPost(long userId, String content) {
		return insertTextPost(userId, content, "PRIVATE");
	}

	private long insertTextPost(long userId, String content, String visibility) {
		jdbcTemplate.update("""
				INSERT INTO social_posts (
					user_id, author_snapshot_nickname, author_anonymized, post_type,
					visibility, status, content, created_at, updated_at
				)
				VALUES (?, '책리더', FALSE, 'TEXT', ?, 'ACTIVE', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId, visibility, content);
		return jdbcTemplate.queryForObject("SELECT MAX(id) FROM social_posts", Long.class);
	}

	private void createSocialTables() {
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
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_social_posts_user_idempotency UNIQUE (user_id, idempotency_key)
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
				CREATE TABLE IF NOT EXISTS user_blocks (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					blocker_user_id BIGINT NOT NULL,
					blocked_user_id BIGINT NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_user_blocks_blocker_blocked UNIQUE (blocker_user_id, blocked_user_id)
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
	}
}
