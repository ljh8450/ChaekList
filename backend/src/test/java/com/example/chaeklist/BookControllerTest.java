package com.example.chaeklist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chaeklist.domain.book.service.BookImagePredeployRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
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

	@Autowired
	private BookImagePredeployRunner bookImagePredeployRunner;

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
	@Transactional
	void returnsKeywordTrendsWithRepresentativeBooks() throws Exception {
		insertCategory(601, "경제");
		insertKeyword(611, "투자");
		insertKeyword(612, "AI");
		insertBook(621, "투자 대표 책", true);
		insertBook(622, "AI 대표 책", true);
		insertBook(623, "제외될 수험서", false);
		insertBookCategory(621, 601);
		insertBookCategory(622, 601);
		insertBookCategory(623, 601);
		insertBookKeyword(621, 611);
		insertBookKeyword(622, 612);
		insertBookKeyword(623, 611);
		insertRankingSnapshot(631, 621, null, 2);
		insertRankingSnapshot(632, 622, null, 1);

		mockMvc.perform(get("/api/books/trends/keywords")
						.param("limit", "2")
						.param("booksPerKeyword", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].keyword", is("AI")))
				.andExpect(jsonPath("$[0].bookCount", is(1)))
				.andExpect(jsonPath("$[0].trendScore", is("+15%")))
				.andExpect(jsonPath("$[0].books", hasSize(1)))
				.andExpect(jsonPath("$[0].books[0].id", is("622")))
				.andExpect(jsonPath("$[1].keyword", is("투자")))
				.andExpect(jsonPath("$[1].bookCount", is(1)))
				.andExpect(jsonPath("$[1].books[?(@.id == '623')]", hasSize(0)));
	}

	@Test
	@Transactional
	void excludesNonTrendKeywordsFromKeywordTrends() throws Exception {
		insertCategory(641, "경제");
		insertKeyword(651, "투자");
		insertKeywordWithType(652, "기출", "EXCLUDE");
		insertBook(661, "투자 트렌드 책", true);
		insertBook(662, "기출 키워드가 붙은 교양 책", true);
		insertBookCategory(661, 641);
		insertBookCategory(662, 641);
		insertBookKeyword(661, 651);
		insertBookKeyword(662, 652);
		insertRankingSnapshot(671, 661, null, 1);
		insertRankingSnapshot(672, 662, null, 2);

		mockMvc.perform(get("/api/books/trends/keywords")
						.param("limit", "5")
						.param("booksPerKeyword", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.keyword == '투자')]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.keyword == '기출')]", hasSize(0)))
				.andExpect(jsonPath("$[0].books[?(@.id == '662')]", hasSize(0)));
	}

	@Test
	@Transactional
	void usesOnlyTrendKeywordWhenSameKeywordNameHasDifferentTypes() throws Exception {
		insertCategory(681, "경제");
		insertKeywordWithType(691, "투자", "TREND");
		insertKeywordWithType(692, "투자", "GENERAL");
		insertBook(701, "트렌드 투자 책", true);
		insertBook(702, "일반 투자 키워드 책", true);
		insertBookCategory(701, 681);
		insertBookCategory(702, 681);
		insertBookKeyword(701, 691);
		insertBookKeyword(702, 692);
		insertRankingSnapshot(711, 701, null, 1);
		insertRankingSnapshot(712, 702, null, 2);

		mockMvc.perform(get("/api/books/trends/keywords")
						.param("limit", "5")
						.param("booksPerKeyword", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.keyword == '투자')]", hasSize(1)))
				.andExpect(jsonPath("$[0].keyword", is("투자")))
				.andExpect(jsonPath("$[0].bookCount", is(1)))
				.andExpect(jsonPath("$[0].books[?(@.id == '701')]", hasSize(1)))
				.andExpect(jsonPath("$[0].books[?(@.id == '702')]", hasSize(0)));
	}

	@Test
	void returnsEmptyKeywordTrends() throws Exception {
		mockMvc.perform(get("/api/books/trends/keywords"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void returnsCategories() throws Exception {
		mockMvc.perform(get("/api/books/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@Transactional
	void searchesBooksByTitle() throws Exception {
		insertCategory(701, "경제");
		insertBook(711, "조용한 투자 습관", true);
		insertBook(712, "투자 제외 도서", false);
		insertBook(713, "느리게 읽는 힘", true);
		insertBookCategory(711, 701);
		insertBookCategory(712, 701);
		insertBookCategory(713, 701);

		mockMvc.perform(get("/api/books/search")
						.param("query", "투자")
						.param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is("711")))
				.andExpect(jsonPath("$[0].title", is("조용한 투자 습관")))
				.andExpect(jsonPath("$[0].category", is("경제")))
				.andExpect(jsonPath("$[0].recommendationReason",
						is("최근 경제 분야에서 교양 필터를 통과해 추천됩니다.")));
	}

	@Test
	@Transactional
	void searchesBooksByAuthor() throws Exception {
		insertBookWithAuthor(721, "철학 입문", "김투자", true);
		insertBookWithAuthor(722, "경제 입문", "다른 저자", true);

		mockMvc.perform(get("/api/books/search")
						.param("query", "김투자"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is("721")))
				.andExpect(jsonPath("$[0].author", is("김투자")));
	}

	@Test
	@Transactional
	void returnsEmptySearchResult() throws Exception {
		insertBook(731, "읽을 만한 책", true);

		mockMvc.perform(get("/api/books/search")
						.param("query", "없는검색어"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@Transactional
	void replacesCallbackStyleSeedCoverUrlWithLocalAsset() {
		insertSeedBook(741, "Slow Reading", "slow-reading", "LOCAL", "https://example.com/book-cover/callback?id=slow-reading");

		bookImagePredeployRunner.run(null);

		String coverImageUrl = jdbcTemplate.queryForObject("""
				SELECT cover_image_url
				FROM books
				WHERE id = ?
				""", String.class, 741);
		org.assertj.core.api.Assertions.assertThat(coverImageUrl).isEqualTo("/book-covers/slow-reading.svg");
	}

	@Test
	@Transactional
	void keepsKakaoSeedCoverUrlDuringPredeploy() {
		insertSeedBook(742, "Slow Reading", "slow-reading", "LOCAL", "https://img.example.com/slow-reading.jpg");

		bookImagePredeployRunner.run(null);

		String coverImageUrl = jdbcTemplate.queryForObject("""
				SELECT cover_image_url
				FROM books
				WHERE id = ?
				""", String.class, 742);
		org.assertj.core.api.Assertions.assertThat(coverImageUrl).isEqualTo("https://img.example.com/slow-reading.jpg");
	}

	@Test
	void rejectsShortSearchQuery() throws Exception {
		mockMvc.perform(get("/api/books/search")
						.param("query", "투"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Search query must be at least 2 characters.")));
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
				.andExpect(jsonPath("$.saved", is(false)))
				.andExpect(jsonPath("$.read", is(false)))
				.andExpect(jsonPath("$.dismissed", is(false)))
				.andExpect(jsonPath("$.keywords", hasSize(1)))
				.andExpect(jsonPath("$.keywords[0]", is("투자")))
				.andExpect(jsonPath("$.filterReport.status", is("INCLUDED")))
				.andExpect(jsonPath("$.filterReport.reason", is("교양 필터 통과")))
				.andExpect(jsonPath("$.filterReport.category", is("경제")))
				.andExpect(jsonPath("$.filterReport.keywords[0]", is("투자")))
				.andExpect(jsonPath("$.recommendationEvidence", hasSize(3)))
				.andExpect(jsonPath("$.recommendationEvidence[0].type", is("CATEGORY")))
				.andExpect(jsonPath("$.recommendationEvidence[0].label", is("대표 분야")))
				.andExpect(jsonPath("$.recommendationEvidence[0].description",
						is("경제 분야의 교양 도서를 찾을 때 비교할 수 있는 후보입니다.")))
				.andExpect(jsonPath("$.recommendationEvidence[1].type", is("KEYWORD")))
				.andExpect(jsonPath("$.recommendationEvidence[2].type", is("FILTER")))
				.andExpect(jsonPath("$.readingGuide.fit",
						is("경제 분야에서 투자 키워드를 기준으로 다음 읽을 책을 고르는 사용자에게 맞습니다.")))
				.andExpect(jsonPath("$.readingGuide.similarityNote",
						is("비슷한 책과 일부 키워드를 공유해 함께 비교해 볼 수 있습니다.")))
				.andExpect(jsonPath("$.similarBooks", hasSize(3)))
				.andExpect(jsonPath("$.similarBooks[0].id", is("302")))
				.andExpect(jsonPath("$.similarBooks[?(@.id == '301')]", hasSize(0)))
				.andExpect(jsonPath("$.similarBooks[?(@.id == '306')]", hasSize(0)));
	}

	@Test
	@Transactional
	void returnsBookDetailWithEmptyOptionalGuideData() throws Exception {
		insertBook(312, "분류 없는 책", true);

		mockMvc.perform(get("/api/books/{bookId}", "312"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is("312")))
				.andExpect(jsonPath("$.category", is("미분류")))
				.andExpect(jsonPath("$.filterReport.status", is("INCLUDED")))
				.andExpect(jsonPath("$.filterReport.reason", is("교양 필터 통과")))
				.andExpect(jsonPath("$.filterReport.category", nullValue()))
				.andExpect(jsonPath("$.filterReport.keywords", hasSize(0)))
				.andExpect(jsonPath("$.recommendationEvidence", hasSize(1)))
				.andExpect(jsonPath("$.recommendationEvidence[0].type", is("FILTER")))
				.andExpect(jsonPath("$.readingGuide", nullValue()))
				.andExpect(jsonPath("$.similarBooks", hasSize(0)));
	}

	@Test
	@Transactional
	void returnsBookDetailWithAuthenticatedInteractionState() throws Exception {
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(111, "인문");
		insertBook(311, "저장하고 읽은 책", true);
		insertBookCategory(311, 111);
		insertInteraction(userId, 311, "SAVE", "2026-04-22 10:00:00");
		insertInteraction(userId, 311, "READ", "2026-04-22 10:01:00");
		insertInteraction(userId, 311, "DISMISS", "2026-04-22 10:02:00");

		mockMvc.perform(get("/api/books/{bookId}", "311")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is("311")))
				.andExpect(jsonPath("$.saved", is(true)))
				.andExpect(jsonPath("$.read", is(true)))
				.andExpect(jsonPath("$.dismissed", is(true)));
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
	@Transactional
	void returnsPersonalHomeWithAccessToken() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		String accessToken = loginAndExtractAccessToken();

		mockMvc.perform(get("/api/me/home")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personalized", is(true)))
				.andExpect(jsonPath("$.todayRecommendation", nullValue()));
	}

	@Test
	@Transactional
	void returnsPersonalHomeRecommendationFromInterestCategory() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(401, "경제");
		insertCategory(402, "인문");
		insertBook(411, "경제 후보", true);
		insertBook(412, "인문 후보", true);
		insertBookCategory(411, 401);
		insertBookCategory(412, 402);
		insertInterestCategory(userId, 401);

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personalized", is(true)))
				.andExpect(jsonPath("$.todayRecommendation.id", is("411")))
				.andExpect(jsonPath("$.todayRecommendation.recommendationReason",
						is("관심 분야로 선택한 경제 분야의 교양 도서입니다.")));

		org.assertj.core.api.Assertions.assertThat(countRecommendations(userId, 411)).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(recommendationReason(userId, 411))
				.isEqualTo("관심 분야로 선택한 경제 분야의 교양 도서입니다.");
		java.time.LocalDateTime firstCreatedAt = recommendationCreatedAt(userId, 411);

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("411")));

		org.assertj.core.api.Assertions.assertThat(countRecommendations(userId, 411)).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(recommendationCreatedAt(userId, 411)).isEqualTo(firstCreatedAt);
	}

	@Test
	@Transactional
	void returnsPersonalHomeRecommendationFromReadBookKeyword() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(421, "경제");
		insertCategory(422, "인문");
		insertKeyword(431, "투자");
		insertBook(441, "읽은 투자 책", true);
		insertBook(442, "투자 키워드 후보", true);
		insertBook(443, "일반 후보", true);
		insertBookCategory(441, 421);
		insertBookCategory(442, 422);
		insertBookCategory(443, 422);
		insertBookKeyword(441, 431);
		insertBookKeyword(442, 431);
		insertInteraction(userId, 441, "READ", "2026-04-22 10:00:00");

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("442")))
				.andExpect(jsonPath("$.todayRecommendation.recommendationReason",
						is("읽은 책과 투자 키워드를 공유해 다음 독서 후보로 추천합니다.")));
	}

	@Test
	@Transactional
	void returnsPersonalHomeRecommendationFromSavedBookCategory() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(501, "경제");
		insertCategory(502, "인문");
		insertKeyword(511, "투자");
		insertBook(521, "저장한 투자 책", true);
		insertBook(522, "저장 기반 경제 후보", true);
		insertBook(523, "다른 분야 후보", true);
		insertBookCategory(521, 501);
		insertBookCategory(522, 501);
		insertBookCategory(523, 502);
		insertBookKeyword(521, 511);
		insertBookKeyword(522, 511);
		insertInteraction(userId, 521, "SAVE", "2026-04-22 10:00:00");

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("522")))
				.andExpect(jsonPath("$.todayRecommendation.recommendationReason",
						is("저장한 책과 투자 키워드를 공유합니다.")));
	}

	@Test
	@Transactional
	void returnsPersonalHomeRecommendationFromReadingPurpose() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(581, "소설");
		insertCategory(582, "경제");
		insertBook(591, "가벼운 소설 후보", true);
		insertBook(592, "경제 후보", true);
		insertBookCategory(591, 581);
		insertBookCategory(592, 582);
		insertReadingPurpose(userId, "LIGHT_READING");

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("591")))
				.andExpect(jsonPath("$.todayRecommendation.recommendationReason",
						is("가벼운 독서 목적에 맞는 소설 분야의 교양 도서입니다.")));
	}

	@Test
	@Transactional
	void ignoresUnsavedBookForPersonalHomeRecommendation() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(531, "경제");
		insertKeyword(541, "투자");
		insertBook(551, "저장 취소한 책", true);
		insertBook(552, "저장 취소 기반 후보", true);
		insertBook(553, "랭킹 fallback 후보", true);
		insertBookCategory(551, 531);
		insertBookCategory(552, 531);
		insertBookCategory(553, 531);
		insertBookKeyword(551, 541);
		insertBookKeyword(552, 541);
		insertInteraction(userId, 551, "SAVE", "2026-04-22 10:00:00");
		insertInteraction(userId, 551, "UNSAVE", "2026-04-22 10:01:00");
		insertRankingSnapshot(561, 553, null, 1);

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("553")))
				.andExpect(jsonPath("$.todayRecommendation.recommendationReason",
						is("랭킹 지표와 교양 필터링 기준을 반영한 책입니다.")));
	}

	@Test
	@Transactional
	void excludesDismissedBookFromPersonalHomeRecommendation() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		long userId = userId();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(451, "경제");
		insertBook(461, "대체 후보", true);
		insertBook(462, "관심 없음 후보", true);
		insertBookCategory(461, 451);
		insertBookCategory(462, 451);
		insertInterestCategory(userId, 451);
		insertInteraction(userId, 462, "DISMISS", "2026-04-22 10:00:00");

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("461")));
	}

	@Test
	@Transactional
	void fallsBackToRankingRecommendationWhenPersonalCandidatesAreMissing() throws Exception {
		createUserInterestCategoriesTable();
		createUserBookInteractionsTable();
		String accessToken = loginAndExtractAccessToken();

		insertCategory(471, "경제");
		insertBook(481, "랭킹 후보", true);
		insertBookCategory(481, 471);
		insertRankingSnapshot(491, 481, null, 1);

		mockMvc.perform(get("/api/me/home")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayRecommendation.id", is("481")))
				.andExpect(jsonPath("$.todayRecommendation.recommendationReason",
						is("랭킹 지표와 교양 필터링 기준을 반영한 책입니다.")));
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
		createRecommendationsTable();
	}

	private void createUserInterestCategoriesTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_interest_categories (
					id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
					user_id BIGINT NOT NULL,
					category_id BIGINT NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		createUserReadingPurposesTable();
	}

	private void createUserReadingPurposesTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_reading_purposes (
					user_id BIGINT NOT NULL,
					purpose_code VARCHAR(50) NOT NULL,
					created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
					PRIMARY KEY (user_id, purpose_code)
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

	private void insertCategory(long id, String name) {
		jdbcTemplate.update("""
				INSERT INTO categories (id, name, slug, display_order, is_active, created_at, updated_at)
				VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, name, "category-" + id, 1);
	}

	private void insertKeyword(long id, String name) {
		insertKeywordWithType(id, name, "TREND");
	}

	private void insertKeywordWithType(long id, String name, String keywordType) {
		jdbcTemplate.update("""
				INSERT INTO keywords (id, name, keyword_type, created_at)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP)
				""", id, name, keywordType);
	}

	private void insertBook(long id, String title, boolean generalEligible) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, '테스트 저자', '상세 설명', ?, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title, generalEligible);
	}

	private void insertBookWithAuthor(long id, String title, String author, boolean generalEligible) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, ?, '상세 설명', ?, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title, author, generalEligible);
	}

	private void insertSeedBook(long id, String title, String sourceBookId, String sourceProvider, String coverImageUrl) {
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, description, cover_image_url, source_provider, source_book_id,
					is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (?, ?, '테스트 저자', '상세 설명', ?, ?, ?, TRUE, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, title, coverImageUrl, sourceProvider, sourceBookId);
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

	private void insertInteraction(long userId, long bookId, String interactionType, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO user_book_interactions (user_id, book_id, interaction_type, created_at)
				VALUES (?, ?, ?, ?)
				""", userId, bookId, interactionType, createdAt);
	}

	private void insertInterestCategory(long userId, long categoryId) {
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

	private void insertRankingSnapshot(long id, long bookId, Long categoryId, int rankPosition) {
		jdbcTemplate.update("""
				INSERT INTO book_ranking_snapshots (
					id,
					book_id,
					category_id,
					ranking_period,
					rank_date,
					rank_position,
					ranking_score,
					view_count,
					click_count,
					save_count,
					review_count,
					recent_growth_rate,
					created_at
				)
				VALUES (?, ?, ?, 'WEEKLY', DATE '2026-04-20', ?, 95.0000, 2400, 500, 120, 20, 15.0000, CURRENT_TIMESTAMP)
				""", id, bookId, categoryId, rankPosition);
	}

	private void createRecommendationsTable() {
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
	}

	private int countRecommendations(long userId, long bookId) {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM recommendations
				WHERE user_id = ?
					AND book_id = ?
					AND recommendation_type = 'CONTENT_BASED'
				""", Integer.class, userId, bookId);
	}

	private String recommendationReason(long userId, long bookId) {
		return jdbcTemplate.queryForObject("""
				SELECT reason
				FROM recommendations
				WHERE user_id = ?
					AND book_id = ?
					AND recommendation_type = 'CONTENT_BASED'
				""", String.class, userId, bookId);
	}

	private java.time.LocalDateTime recommendationCreatedAt(long userId, long bookId) {
		java.sql.Timestamp timestamp = jdbcTemplate.queryForObject("""
				SELECT created_at
				FROM recommendations
				WHERE user_id = ?
					AND book_id = ?
					AND recommendation_type = 'CONTENT_BASED'
				""", java.sql.Timestamp.class, userId, bookId);
		return timestamp.toLocalDateTime();
	}
}
