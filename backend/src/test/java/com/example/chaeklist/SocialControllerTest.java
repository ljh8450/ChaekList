package com.example.chaeklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
	void filtersAndSortsPublicFeed() throws Exception {
		createSocialTables();
		long userId = userId();
		long textPostId = insertPublicTextPostAt(userId, "최신 자유 글", "2026-04-27 10:00:00");
		long growthPostId = insertPostAt(userId, "좋아요 많은 성장 카드", "PUBLIC", "READING_GROWTH", "2026-04-26 10:00:00");
		insertPostLike(growthPostId, 90001);
		insertPostLike(growthPostId, 90002);

		mockMvc.perform(get("/api/social/feed")
						.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is((int) textPostId)))
				.andExpect(jsonPath("$[1].id", is((int) growthPostId)));

		mockMvc.perform(get("/api/social/feed")
						.param("sort", "likes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is((int) growthPostId)))
				.andExpect(jsonPath("$[0].likeCount", is(2)))
				.andExpect(jsonPath("$[1].id", is((int) textPostId)));

		mockMvc.perform(get("/api/social/feed")
						.param("type", "TEXT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is((int) textPostId)));
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
	void createsLikeNotificationAndMarksItRead() throws Exception {
		createSocialTables();
		String ownerAccessToken = loginAndExtractAccessToken();
		String likerAccessToken = signupAndExtractAccessToken("liker@chaeklist.kr", "kind-liker");
		long postId = insertPublicTextPost(userId(), "알림을 받을 공개 기록");

		mockMvc.perform(post("/api/social/posts/{postId}/likes", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + likerAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked", is(true)));

		String responseBody = mockMvc.perform(get("/api/me/notifications")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].notificationType", is("LIKE")))
				.andExpect(jsonPath("$[0].targetType", is("POST")))
				.andExpect(jsonPath("$[0].targetId", is((int) postId)))
				.andExpect(jsonPath("$[0].read", is(false)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		long notificationId = objectMapper.readTree(responseBody).get(0).get("id").asLong();
		mockMvc.perform(patch("/api/me/notifications/{notificationId}/read", notificationId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is((int) notificationId)))
				.andExpect(jsonPath("$.read", is(true)));
	}

	@Test
	@Transactional
	void uploadsPostMediaAndReturnsItFromFeed() throws Exception {
		createSocialTables();
		String accessToken = loginAndExtractAccessToken();
		long postId = insertPublicTextPost(userId(), "이미지를 붙일 공개 기록");
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"note.png",
				"image/png",
				new byte[] { 1, 2, 3 }
		);

		String uploadBody = mockMvc.perform(multipart("/api/social/posts/{postId}/media", postId)
						.file(file)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId", is((int) postId)))
				.andExpect(jsonPath("$.fileName", is("note.png")))
				.andExpect(jsonPath("$.contentType", is("image/png")))
				.andExpect(jsonPath("$.sizeBytes", is(3)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		long mediaId = objectMapper.readTree(uploadBody).get("id").asLong();
		mockMvc.perform(get("/api/social/posts/{postId}/media/{mediaId}", postId, mediaId))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(content().bytes(new byte[] { 1, 2, 3 }));

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].media", hasSize(1)))
				.andExpect(jsonPath("$[0].media[0].id", is((int) mediaId)))
				.andExpect(jsonPath("$[0].media[0].url", is("/api/social/posts/" + postId + "/media/" + mediaId)));
	}

	@Test
	@Transactional
	void adminReviewsReportsAndTogglesHiddenPost() throws Exception {
		createSocialTables();
		String reporterAccessToken = loginAndExtractAccessToken();
		String adminAccessToken = insertAdminAndExtractAccessToken("admin@chaeklist.kr", "admin-reader");
		long userId = userId();
		long postId = insertPublicTextPost(userId, "관리자 검토 대상 기록");

		mockMvc.perform(post("/api/social/posts/{postId}/reports", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "SPAM",
								  "detail": "반복 홍보로 보여요."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("PENDING")));

		String reportsBody = mockMvc.perform(get("/api/admin/social/reports")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].targetType", is("POST")))
				.andExpect(jsonPath("$[0].targetId", is((int) postId)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		long reportId = objectMapper.readTree(reportsBody).get(0).get("id").asLong();
		mockMvc.perform(patch("/api/admin/social/reports/{reportId}", reportId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "REVIEWED"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("REVIEWED")));

		mockMvc.perform(get("/api/me/notifications")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].notificationType", is("REPORT_STATUS")))
				.andExpect(jsonPath("$[0].targetType", is("REPORT")))
				.andExpect(jsonPath("$[0].targetId", is((int) reportId)))
				.andExpect(jsonPath("$[0].read", is(false)));

		mockMvc.perform(post("/api/admin/social/posts/{postId}/hide", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "신고 검토 후 숨김"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is((int) postId)));

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(delete("/api/admin/social/posts/{postId}/hide", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is((int) postId)));

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	@Transactional
	void adminAddsReportMemoNicknameActionAndCreatesServiceNotification() throws Exception {
		createSocialTables();
		String reporterAccessToken = loginAndExtractAccessToken();
		String adminAccessToken = insertAdminAndExtractAccessToken("notice-admin@chaeklist.kr", "notice-admin");
		long reportedUserId = insertUser("reported@chaeklist.kr", "bad-nickname");

		String reportBody = mockMvc.perform(post("/api/users/{userId}/reports", reportedUserId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "INAPPROPRIATE_NICKNAME",
								  "detail": "닉네임이 부적절합니다."
								}
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		long reportId = objectMapper.readTree(reportBody).get("id").asLong();
		mockMvc.perform(patch("/api/admin/social/reports/{reportId}", reportId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "REVIEWED",
								  "memo": "닉네임 변경 요청 대상",
								  "nicknameAction": "REQUIRE_CHANGE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("REVIEWED")));

		mockMvc.perform(get("/api/admin/social/reports/{reportId}/events", reportId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].eventType", is("STATUS_CHANGED")))
				.andExpect(jsonPath("$[0].memo", is("닉네임 변경 요청 대상")))
				.andExpect(jsonPath("$[1].eventType", is("NICKNAME_REQUIRE_CHANGE")));

		mockMvc.perform(post("/api/admin/notifications/service")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "audience": "USER",
								  "userId": %d,
								  "title": "서비스 공지",
								  "message": "닉네임 정책을 확인해주세요."
								}
								""".formatted(reportedUserId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.audience", is("USER")))
				.andExpect(jsonPath("$.userId", is((int) reportedUserId)))
				.andExpect(jsonPath("$.deliveredCount", is(1)));
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
	void withdrawsUserAnonymizesPublicPostsAndRejectsExistingToken() throws Exception {
		createSocialTables();
		createUserPublicProfilesTable();
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		long postId = insertPublicTextPost(userId, "탈퇴 후에도 남을 공개 기록");
		insertPublicProfile(userId);

		mockMvc.perform(post("/api/me/withdraw")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNoContent());

		UserWithdrawalRow userRow = jdbcTemplate.queryForObject("""
				SELECT email, nickname, password_hash, status
				FROM users
				WHERE id = ?
				""", (resultSet, rowNumber) -> new UserWithdrawalRow(
						resultSet.getString("email"),
						resultSet.getString("nickname"),
						resultSet.getString("password_hash"),
						resultSet.getString("status")
				), userId);
		assertThat(userRow.email()).isEqualTo("deleted_" + userId + "@deleted.local");
		assertThat(userRow.nickname()).isEqualTo("deleted_" + userId);
		assertThat(userRow.passwordHash()).isEmpty();
		assertThat(userRow.status()).isEqualTo("DELETED");

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is((int) postId)))
				.andExpect(jsonPath("$[0].userId").doesNotExist())
				.andExpect(jsonPath("$[0].nickname", is("탈퇴한 사용자")))
				.andExpect(jsonPath("$[0].authorAnonymized", is(true)));

		mockMvc.perform(get("/api/users/{userId}/public-profile", userId))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/me/settings")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isUnauthorized());
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

	@Test
	@Transactional
	void allowsSavedBookShareAfterResave() throws Exception {
		createSocialTables();
		createUserBookInteractionsTable();
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertInteraction(userId, 901, "SAVE", "2026-04-22 10:00:00");
		insertInteraction(userId, 901, "UNSAVE", "2026-04-22 10:01:00");
		insertInteraction(userId, 901, "SAVE", "2026-04-22 10:02:00");

		mockMvc.perform(post("/api/social/posts")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "postType": "SAVED_BOOK",
								  "bookId": 901,
								  "visibility": "PUBLIC",
								  "idempotencyKey": "resaved-book"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postType", is("SAVED_BOOK")))
				.andExpect(jsonPath("$.visibility", is("PUBLIC")));
	}

	@Test
	@Transactional
	void allowsCompletedReadingRoomShareAndReport() throws Exception {
		createSocialTables();
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertBook(9041, "모각독 공유 책");
		long roomId = insertCompletedReadingRoom(userId, 9041);

		mockMvc.perform(post("/api/social/posts")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "postType": "READING_ROOM",
								  "sourceInteractionId": %d,
								  "content": "모각독 인증을 마쳤습니다.",
								  "visibility": "PUBLIC",
								  "idempotencyKey": "reading-room-share"
								}
								""".formatted(roomId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postType", is("READING_ROOM")))
				.andExpect(jsonPath("$.content", is("모각독 인증을 마쳤습니다.")));

		mockMvc.perform(post("/api/reading-rooms/{roomId}/reports", roomId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "OTHER",
								  "detail": "모각독 신고 테스트"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetType", is("READING_ROOM")))
				.andExpect(jsonPath("$.targetId", is((int) roomId)));
	}

	@Test
	@Transactional
	void excludesHiddenPostsFromPublicProfileAndUserSearchCounts() throws Exception {
		createSocialTables();
		createUserPublicProfilesTable();
		long userId = userId();
		insertPublicTextPost(userId, "보이는 공개 기록");
		long hiddenPostId = insertPublicTextPost(userId, "숨김 공개 기록");
		hidePost(hiddenPostId);

		mockMvc.perform(get("/api/users/{userId}/public-profile", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publicPostCount", is(1)));

		mockMvc.perform(get("/api/search/users")
						.param("query", "quiet"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].summary", containsString("공개 게시글 1개")));
	}

	@Test
	@Transactional
	void improvesPublicUserSearchSummaryAndOrdering() throws Exception {
		createSocialTables();
		createUserPublicProfilesTable();
		long userId = userId();
		long directMatchUserId = insertUser("direct@chaeklist.kr", "quiet");
		insertPublicTextPostAt(userId, "quiet 독서 모임 기록", "2026-04-25 10:00:00");
		insertPublicTextPostAt(userId, "quiet 검색 보조 기록", "2026-04-26 10:00:00");
		insertPublicTextPostAt(directMatchUserId, "직접 매칭 유저 공개 기록", "2026-04-20 10:00:00");
		insertPublicProfile(directMatchUserId);
		insertCategory(9901, "경제", "test-economy", 1);
		insertCategory(9902, "철학", "test-philosophy", 2);
		setInterestsPublic(directMatchUserId);
		insertUserInterest(directMatchUserId, 9901);
		insertUserInterest(directMatchUserId, 9902);

		mockMvc.perform(get("/api/search/users")
						.param("query", "quiet"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].title", is("quiet")))
				.andExpect(jsonPath("$[0].summary", containsString("공개 게시글 1개")))
				.andExpect(jsonPath("$[0].summary", containsString("관심 분야 경제, 철학")))
				.andExpect(jsonPath("$[0].summary", containsString("최근 활동 2026-04-20")))
				.andExpect(jsonPath("$[1].title", is("quiet-reader")))
				.andExpect(jsonPath("$[1].summary", containsString("공개 게시글 2개")))
				.andExpect(jsonPath("$[1].summary", containsString("최근 활동 2026-04-26")));
	}

	@Test
	@Transactional
	void returnsPublicProfilePostsOnly() throws Exception {
		createSocialTables();
		createUserPublicProfilesTable();
		long userId = userId();
		insertPublicTextPost(userId, "프로필 공개 기록");
		insertPost(userId, "성장 카드 공개 기록", "PUBLIC", "READING_GROWTH");
		insertPrivateTextPost(userId, "프로필 비공개 기록");
		long hiddenPostId = insertPublicTextPost(userId, "프로필 숨김 기록");
		hidePost(hiddenPostId);

		mockMvc.perform(get("/api/users/{userId}/social/posts", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.content == '프로필 공개 기록')]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.content == '성장 카드 공개 기록')]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.content == '프로필 비공개 기록')]", hasSize(0)))
				.andExpect(jsonPath("$[?(@.content == '프로필 숨김 기록')]", hasSize(0)));

		mockMvc.perform(get("/api/users/{userId}/social/posts", userId)
						.param("type", "TEXT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].content", is("프로필 공개 기록")));
	}

	@Test
	@Transactional
	void canMakePrivatePostPublicAgain() throws Exception {
		createSocialTables();
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		long postId = insertPrivateTextPost(userId, "공개로 돌릴 기록");

		mockMvc.perform(patch("/api/social/posts/{postId}", postId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "visibility": "PUBLIC"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility", is("PUBLIC")));

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is((int) postId)));
	}

	@Test
	@Transactional
	void showsPublicPrimaryBadgeOnPublicSocialSurfaces() throws Exception {
		createSocialTables();
		createUserBookInteractionsTable();
		createUserPublicProfilesTable();
		long userId = userId();
		setBadgesPublic(userId);
		insertInteraction(userId, 903, "READ", "2026-04-23 10:00:00");
		insertPublicTextPost(userId, "대표 배지 공개 기록");

		mockMvc.perform(get("/api/social/feed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].primaryBadge.code", is("FIRST_READ")))
				.andExpect(jsonPath("$[0].primaryBadge.label", is("첫 독서 기록")));

		mockMvc.perform(get("/api/users/{userId}/public-profile", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.primaryBadge.code", is("FIRST_READ")));

		mockMvc.perform(get("/api/search/users")
						.param("query", "quiet"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].primaryBadge.code", is("FIRST_READ")));
	}

	@Test
	@Transactional
	void searchesPublicUsersByPublicPostContent() throws Exception {
		createSocialTables();
		createUserPublicProfilesTable();
		long userId = userId();
		insertPublicTextPost(userId, "느린 독서 루틴을 공유합니다");

		mockMvc.perform(get("/api/search/users")
						.param("query", "루틴"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title", is("quiet-reader")));
	}

	@Test
	@Transactional
	void returnsPublicFeedWhenBearerTokenIsInvalid() throws Exception {
		createSocialTables();
		insertPublicTextPost(userId(), "토큰이 없어도 볼 공개 기록");

		mockMvc.perform(get("/api/social/feed")
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].likedByMe", is(false)));
	}

	@Test
	@Transactional
	void returnsMyPostsAndLikedPosts() throws Exception {
		createSocialTables();
		String accessToken = loginAndExtractAccessToken();
		long userId = userId();
		insertPrivateTextPost(userId, "내 비공개 기록");
		long publicPostId = insertPublicTextPost(userId, "내 공개 기록");

		mockMvc.perform(post("/api/social/posts/{postId}/likes", publicPostId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/me/social/posts")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].mine", is(true)))
				.andExpect(jsonPath("$[?(@.content == '내 비공개 기록')]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.content == '내 공개 기록')]", hasSize(1)));

		mockMvc.perform(get("/api/me/social/liked-posts")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].content", is("내 공개 기록")))
				.andExpect(jsonPath("$[0].likedByMe", is(true)));
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

	private long insertPublicTextPost(long userId, String content) {
		return insertTextPost(userId, content, "PUBLIC");
	}

	private long insertPrivateTextPost(long userId, String content) {
		return insertTextPost(userId, content, "PRIVATE");
	}

	private long insertPublicTextPostAt(long userId, String content, String createdAt) {
		return insertPostAt(userId, content, "PUBLIC", "TEXT", createdAt);
	}

	private long insertTextPost(long userId, String content, String visibility) {
		return insertPost(userId, content, visibility, "TEXT");
	}

	private long insertPost(long userId, String content, String visibility, String postType) {
		return insertPostAt(userId, content, visibility, postType, null);
	}

	private long insertPostAt(long userId, String content, String visibility, String postType, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO social_posts (
					user_id, author_snapshot_nickname, author_anonymized, post_type,
					visibility, status, content, created_at, updated_at
				)
				VALUES (?, '책리더', FALSE, ?, ?, 'ACTIVE', ?, COALESCE(?, CURRENT_TIMESTAMP), COALESCE(?, CURRENT_TIMESTAMP))
				""", userId, postType, visibility, content, createdAt, createdAt);
		return jdbcTemplate.queryForObject("SELECT MAX(id) FROM social_posts", Long.class);
	}

	private long insertUser(String email, String nickname) {
		jdbcTemplate.update("""
				INSERT INTO users (
					email, nickname, password_hash, status, role, onboarding_completed, created_at, updated_at
				)
				VALUES (?, ?, 'test-hash', 'ACTIVE', 'USER', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", email, nickname);
		return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
	}

	private void insertInteraction(long userId, long bookId, String interactionType, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO user_book_interactions (user_id, book_id, interaction_type, created_at)
				VALUES (?, ?, ?, ?)
				""", userId, bookId, interactionType, createdAt);
	}

	private void insertPublicProfile(long userId) {
		jdbcTemplate.update("""
				INSERT INTO user_public_profiles (
					user_id, profile_public, growth_summary_public, public_post_count, created_at, updated_at
				)
				VALUES (?, TRUE, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
	}

	private void setBadgesPublic(long userId) {
		jdbcTemplate.update("DELETE FROM user_privacy_settings WHERE user_id = ?", userId);
		jdbcTemplate.update("""
				INSERT INTO user_privacy_settings (
					user_id, read_books_visibility, saved_books_visibility, reading_growth_visibility,
					badges_visibility, interest_categories_visibility, created_at, updated_at
				)
				VALUES (?, 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PUBLIC', 'PRIVATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
	}

	private void setInterestsPublic(long userId) {
		jdbcTemplate.update("DELETE FROM user_privacy_settings WHERE user_id = ?", userId);
		jdbcTemplate.update("""
				INSERT INTO user_privacy_settings (
					user_id, read_books_visibility, saved_books_visibility, reading_growth_visibility,
					badges_visibility, interest_categories_visibility, created_at, updated_at
				)
				VALUES (?, 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
	}

	private void insertCategory(long id, String name, String slug, int displayOrder) {
		jdbcTemplate.update("""
				INSERT INTO categories (id, name, slug, display_order, is_active, created_at, updated_at)
				VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, name, slug, displayOrder);
	}

	private void insertUserInterest(long userId, long categoryId) {
		jdbcTemplate.update("""
				INSERT INTO user_interest_categories (user_id, category_id, created_at)
				VALUES (?, ?, CURRENT_TIMESTAMP)
				""", userId, categoryId);
	}

	private void insertPostLike(long postId, long userId) {
		jdbcTemplate.update("""
				INSERT INTO social_post_likes (post_id, user_id, created_at)
				VALUES (?, ?, CURRENT_TIMESTAMP)
				""", postId, userId);
	}

	private void hidePost(long postId) {
		jdbcTemplate.update("""
				INSERT INTO social_admin_hidden_posts (post_id, reason, created_at)
				VALUES (?, 'test hidden', CURRENT_TIMESTAMP)
				""", postId);
	}

	private void createUserBookInteractionsTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_book_interactions (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					user_id BIGINT NOT NULL,
					book_id BIGINT NOT NULL,
					interaction_type VARCHAR(30) NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
	}

	private void createUserPublicProfilesTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_public_profiles (
					user_id BIGINT PRIMARY KEY,
					profile_public BOOLEAN NOT NULL DEFAULT FALSE,
					growth_summary_public BOOLEAN NOT NULL DEFAULT FALSE,
					public_post_count INT NOT NULL DEFAULT 0,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
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
				CREATE TABLE IF NOT EXISTS social_post_media (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					post_id BIGINT NOT NULL,
					uploader_user_id BIGINT NOT NULL,
					file_name VARCHAR(255),
					content_type VARCHAR(100) NOT NULL,
					size_bytes BIGINT NOT NULL,
					data BLOB NOT NULL,
					sort_order INT NOT NULL DEFAULT 0,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
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
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS social_reports (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					reporter_user_id BIGINT NOT NULL,
					target_type VARCHAR(30) NOT NULL,
					target_id BIGINT NOT NULL,
					reason VARCHAR(50) NOT NULL,
					detail VARCHAR(500),
					status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					CONSTRAINT uk_social_reports_reporter_target UNIQUE (reporter_user_id, target_type, target_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS social_report_events (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					report_id BIGINT NOT NULL,
					admin_user_id BIGINT NOT NULL,
					event_type VARCHAR(40) NOT NULL,
					from_status VARCHAR(30),
					to_status VARCHAR(30),
					memo VARCHAR(1000),
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_privacy_settings (
					user_id BIGINT PRIMARY KEY,
					read_books_visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
					saved_books_visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
					reading_growth_visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
					badges_visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
					interest_categories_visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
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
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS service_notices (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					created_by_user_id BIGINT NOT NULL,
					audience VARCHAR(20) NOT NULL,
					target_user_id BIGINT,
					title VARCHAR(100) NOT NULL,
					message VARCHAR(255) NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_interest_categories (
					user_id BIGINT NOT NULL,
					category_id BIGINT NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					PRIMARY KEY (user_id, category_id)
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
	}

	private void insertBook(long id, String title) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, '테스트 저자', '상세 설명', TRUE, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title);
	}

	private long insertCompletedReadingRoom(long userId, long bookId) {
		jdbcTemplate.update("""
				INSERT INTO reading_rooms (
					host_user_id, book_id, title, max_participants, status, visibility, created_at, updated_at
				)
				VALUES (?, ?, '완료 공유 모각독', 5, 'RECRUITING', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId, bookId);
		long roomId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_rooms", Long.class);
		jdbcTemplate.update("""
				INSERT INTO reading_room_schedules (room_id, day_of_week, day_label, scheduled_time, duration_minutes, created_at)
				VALUES (?, 2, '월요일', CURRENT_TIME, 60, CURRENT_TIMESTAMP)
				""", roomId);
		long scheduleId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_room_schedules", Long.class);
		jdbcTemplate.update("""
				INSERT INTO reading_room_sessions (
					room_id, schedule_id, session_date, scheduled_start_at, scheduled_end_at, started_at, ended_at, status, created_at, updated_at
				)
				VALUES (?, ?, CURRENT_DATE, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
					DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 'ENDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", roomId, scheduleId);
		long sessionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reading_room_sessions", Long.class);
		jdbcTemplate.update("""
				INSERT INTO reading_room_participants (room_id, user_id, status, joined_at)
				VALUES (?, ?, 'JOINED', DATEADD('HOUR', -2, CURRENT_TIMESTAMP))
				""", roomId, userId);
		jdbcTemplate.update("""
				INSERT INTO reading_room_checkins (session_id, room_id, user_id, note, created_at)
				VALUES (?, ?, ?, '공유 인증', DATEADD('HOUR', -1, CURRENT_TIMESTAMP))
				""", sessionId, roomId, userId);
		return roomId;
	}

	private record UserWithdrawalRow(
			String email,
			String nickname,
			String passwordHash,
			String status
	) {
	}
}
