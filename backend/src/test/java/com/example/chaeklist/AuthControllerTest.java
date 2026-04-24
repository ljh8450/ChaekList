package com.example.chaeklist;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void signsUpActiveUser() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "new-reader@readpick.kr",
								  "nickname": "new-reader",
								  "password": "readpick123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.email", is("new-reader@readpick.kr")))
				.andExpect(jsonPath("$.nickname", is("new-reader")))
				.andExpect(jsonPath("$.status", is("ACTIVE")));
	}

	@Test
	void rejectsDuplicateEmail() throws Exception {
		String payload = """
				{
				  "email": "duplicate@readpick.kr",
				  "nickname": "duplicate-one",
				  "password": "readpick123"
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
								  "email": "duplicate@readpick.kr",
								  "nickname": "duplicate-two",
								  "password": "readpick123"
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
								  "email": "reader@readpick.kr",
								  "password": "readpick123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("reader@readpick.kr")))
				.andExpect(jsonPath("$.nickname", is("quiet-reader")))
				.andExpect(jsonPath("$.status", is("ACTIVE")));
	}

	@Test
	void rejectsInvalidLogin() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "reader@readpick.kr",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("이메일 또는 비밀번호가 올바르지 않습니다.")));
	}
}
