package com.example.chaeklist;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void signsUpActiveUser() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "new-reader@chaeklist.kr",
								  "nickname": "new-reader",
								  "password": "chaeklist123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.email", is("new-reader@chaeklist.kr")))
				.andExpect(jsonPath("$.nickname", is("new-reader")))
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.role", is("USER")))
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.accessToken", not(emptyString())))
				.andExpect(jsonPath("$.refreshToken", not(emptyString())));
	}

	@Test
	void rejectsDuplicateEmail() throws Exception {
		String payload = """
				{
				  "email": "duplicate@chaeklist.kr",
				  "nickname": "duplicate-one",
				  "password": "chaeklist123"
				}
				""";

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "duplicate@chaeklist.kr",
								  "nickname": "duplicate-two",
								  "password": "chaeklist123"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("이미 가입된 이메일입니다.")));
	}

	@Test
	void logsInSeededDemoUser() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "reader@chaeklist.kr",
								  "password": "chaeklist123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("reader@chaeklist.kr")))
				.andExpect(jsonPath("$.nickname", is("quiet-reader")))
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.role", is("USER")))
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.accessToken", not(emptyString())))
				.andExpect(jsonPath("$.refreshToken", not(emptyString())));
	}

	@Test
	void returnsCurrentUserWithBearerToken() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "reader@chaeklist.kr",
								  "password": "chaeklist123"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = loginResult.getResponse().getContentAsString();
		String accessToken = responseBody.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("reader@chaeklist.kr")))
				.andExpect(jsonPath("$.nickname", is("quiet-reader")))
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.role", is("USER")));
	}

	@Test
	void returnsAdminRoleOnLoginAndMe() throws Exception {
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", "admin-auth@chaeklist.kr");
		jdbcTemplate.update("""
				INSERT INTO users (
					email, nickname, password_hash, status, role, onboarding_completed, created_at, updated_at
				)
				VALUES (?, ?, ?, 'ACTIVE', 'ADMIN', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", "admin-auth@chaeklist.kr", "admin-auth", hashPassword("chaeklist123"));

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "admin-auth@chaeklist.kr",
								  "password": "chaeklist123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role", is("ADMIN")))
				.andReturn();

		String responseBody = loginResult.getResponse().getContentAsString();
		String accessToken = responseBody.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("admin-auth@chaeklist.kr")))
				.andExpect(jsonPath("$.role", is("ADMIN")));
	}

	@Test
	void rejectsMeWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message", is("Bearer token is required.")));
	}

	@Test
	void exposesBearerAuthInOpenApiDocs() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type", is("http")))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat", is("JWT")));
	}

	@Test
	void rejectsInvalidLogin() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "reader@chaeklist.kr",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("이메일 또는 비밀번호가 올바르지 않습니다.")));
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
}
