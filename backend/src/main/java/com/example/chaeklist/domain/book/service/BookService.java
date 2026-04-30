package com.example.chaeklist.domain.book.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.chaeklist.domain.book.dto.BookDetailResponse;
import com.example.chaeklist.domain.book.dto.BookDetailResponse.FilterReport;
import com.example.chaeklist.domain.book.dto.BookDetailResponse.ReadingGuide;
import com.example.chaeklist.domain.book.dto.BookDetailResponse.RecommendationEvidence;
import com.example.chaeklist.domain.book.dto.BookSummaryResponse;
import com.example.chaeklist.domain.book.dto.CategoryRankingResponse;
import com.example.chaeklist.domain.book.dto.HomeResponse;
import com.example.chaeklist.domain.book.dto.KeywordTrendResponse;
import com.example.chaeklist.domain.book.entity.Book;
import com.example.chaeklist.domain.book.entity.BookRankingSnapshot;
import com.example.chaeklist.domain.book.repository.BookRankingSnapshotRepository;
import com.example.chaeklist.domain.book.repository.BookRepository;
import com.example.chaeklist.domain.book.repository.CategoryRepository;
import com.example.chaeklist.domain.mypage.model.ReadingPurpose;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

	private static final Set<String> PERIODS = Set.of("daily", "weekly", "monthly");
	private static final int SIMILAR_BOOK_LIMIT = 3;
	private static final int SIMILAR_BOOK_CANDIDATE_LIMIT = 50;
	private static final int PERSONAL_RECOMMENDATION_CANDIDATE_LIMIT = 50;
	private static final int SEARCH_DEFAULT_LIMIT = 10;
	private static final int SEARCH_MAX_LIMIT = 20;
	private static final int SEARCH_MIN_QUERY_LENGTH = 2;
	private static final int KEYWORD_TREND_DEFAULT_LIMIT = 3;
	private static final int KEYWORD_TREND_MAX_LIMIT = 20;
	private static final int KEYWORD_TREND_BOOK_DEFAULT_LIMIT = 3;
	private static final int KEYWORD_TREND_BOOK_MAX_LIMIT = 10;
	private static final String FALLBACK_RECOMMENDATION_REASON = "랭킹 지표와 교양 필터링 기준을 반영한 책입니다.";

	private final BookRepository bookRepository;
	private final BookRankingSnapshotRepository bookRankingSnapshotRepository;
	private final CategoryRepository categoryRepository;
	private final JdbcTemplate jdbcTemplate;

	public BookService(
			BookRepository bookRepository,
			BookRankingSnapshotRepository bookRankingSnapshotRepository,
			CategoryRepository categoryRepository,
			JdbcTemplate jdbcTemplate
	) {
		this.bookRepository = bookRepository;
		this.bookRankingSnapshotRepository = bookRankingSnapshotRepository;
		this.categoryRepository = categoryRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	public HomeResponse getPublicHome() {
		return createHomeResponse(false, getDefaultRecommendation().map(BookSummaryResponse::from));
	}

	@Transactional
	public HomeResponse getPersonalHome(AuthenticatedUser user) {
		Optional<BookSummaryResponse> recommendation = getPersonalRecommendation(user.id())
				.map(personalizedRecommendation -> {
					saveRecommendation(user.id(), personalizedRecommendation);
					return BookSummaryResponse.from(
							personalizedRecommendation.book(),
							personalizedRecommendation.reason()
					);
				})
				.or(() -> getDefaultRecommendation()
						.map(book -> BookSummaryResponse.from(book, FALLBACK_RECOMMENDATION_REASON)));
		return createHomeResponse(true, recommendation);
	}

	public List<BookSummaryResponse> getRankings(String category, String period, int limit) {
		validateCategory(category, true);
		validatePeriod(period);
		return findRanking(category, period, normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public List<BookSummaryResponse> getTrending(int limit) {
		return bookRankingSnapshotRepository.findLatestTrending(normalizePeriod("weekly"), page(normalizeLimit(limit))).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public List<String> getCategories() {
		return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
				.map(category -> category.name())
				.toList();
	}

	public List<BookSummaryResponse> searchBooks(String query, int limit) {
		String normalizedQuery = normalizeSearchQuery(query);
		return bookRepository.searchGeneralEligibleByTitleOrAuthor(normalizedQuery, pageById(normalizeSearchLimit(limit))).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public List<KeywordTrendResponse> getKeywordTrends(int limit, int booksPerKeyword) {
		int normalizedLimit = normalizeKeywordTrendLimit(limit);
		int normalizedBooksPerKeyword = normalizeKeywordTrendBookLimit(booksPerKeyword);
		String period = normalizePeriod("weekly");

		return jdbcTemplate.query("""
				SELECT
					k.name AS keyword_name,
					COUNT(DISTINCT b.id) AS book_count,
					COALESCE(MAX(latest_snapshot.recent_growth_rate), 0) AS trend_score
				FROM keywords k
				JOIN book_keywords bk ON bk.keyword_id = k.id
				JOIN books b ON b.id = bk.book_id
				LEFT JOIN book_ranking_snapshots latest_snapshot
					ON latest_snapshot.book_id = b.id
					AND latest_snapshot.category_id IS NULL
					AND latest_snapshot.ranking_period = ?
					AND latest_snapshot.rank_date = (
						SELECT MAX(snapshot.rank_date)
						FROM book_ranking_snapshots snapshot
						WHERE snapshot.category_id IS NULL
							AND snapshot.ranking_period = ?
					)
				WHERE b.is_general_eligible = TRUE
					AND k.keyword_type = 'TREND'
				GROUP BY k.id, k.name
				ORDER BY trend_score DESC, book_count DESC, k.name ASC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new KeywordTrend(
						resultSet.getString("keyword_name"),
						resultSet.getInt("book_count"),
						resultSet.getBigDecimal("trend_score")
				),
				period,
				period,
				normalizedLimit
		).stream()
				.map(keywordTrend -> new KeywordTrendResponse(
						keywordTrend.keyword(),
						keywordTrend.bookCount(),
						formatTrendScore(keywordTrend.trendScore()),
						bookRepository.findTrendingBooksByKeyword(
										keywordTrend.keyword(),
										period,
										pageById(normalizedBooksPerKeyword)
								).stream()
								.map(BookSummaryResponse::from)
								.toList()
				))
				.toList();
	}

	public List<BookSummaryResponse> getCategoryRankings(String category, String period, int limit) {
		validateCategory(category, false);
		validatePeriod(period);
		return findRanking(category, period, normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public BookDetailResponse getBookDetail(String bookId) {
		Long id = parseBookId(bookId);
		Book book = bookRepository.findByIdAndGeneralEligibleTrue(id)
				.orElseThrow(() -> new BookNotFoundException("Book not found."));
		List<Book> similarBooks = findSimilarBooks(book);
		return createBookDetailResponse(book, similarBooks, false, false, false);
	}

	public BookDetailResponse getBookDetail(String bookId, AuthenticatedUser user) {
		Long id = parseBookId(bookId);
		Book book = bookRepository.findByIdAndGeneralEligibleTrue(id)
				.orElseThrow(() -> new BookNotFoundException("Book not found."));
		List<Book> similarBooks = findSimilarBooks(book);
		return createBookDetailResponse(book, similarBooks, isSaved(user.id(), id), isRead(user.id(), id), isDismissed(user.id(), id));
	}

	private BookDetailResponse createBookDetailResponse(
			Book book,
			List<Book> similarBooks,
			boolean saved,
			boolean read,
			boolean dismissed
	) {
		return BookDetailResponse.from(
				book,
				similarBooks,
				createFilterReport(book),
				createRecommendationEvidence(book),
				createReadingGuide(book, similarBooks),
				saved,
				read,
				dismissed
		);
	}

	private FilterReport createFilterReport(Book book) {
		return new FilterReport(
				book.filterStatus(),
				book.tag(),
				"미분류".equals(book.category()) ? null : book.category(),
				book.keywords()
		);
	}

	private List<RecommendationEvidence> createRecommendationEvidence(Book book) {
		List<String> keywords = book.keywords();
		ArrayList<RecommendationEvidence> evidence = new ArrayList<>();
		if (!"미분류".equals(book.category())) {
			evidence.add(new RecommendationEvidence(
					"CATEGORY",
					"대표 분야",
					book.category() + " 분야의 교양 도서를 찾을 때 비교할 수 있는 후보입니다."
			));
		}
		if (!keywords.isEmpty()) {
			evidence.add(new RecommendationEvidence(
					"KEYWORD",
					"공통 키워드",
					keywords.getFirst() + " 키워드를 중심으로 탐색할 수 있는 책입니다."
			));
		}
		if (book.generalEligible()) {
			evidence.add(new RecommendationEvidence(
					"FILTER",
					"교양 필터",
					book.tag() + " 기준으로 상세 후보에 포함되었습니다."
			));
		}
		return evidence.stream().limit(3).toList();
	}

	private ReadingGuide createReadingGuide(Book book, List<Book> similarBooks) {
		String fit = createFit(book);
		String similarityNote = createSimilarityNote(book, similarBooks);
		if (fit == null && similarityNote == null) {
			return null;
		}
		return new ReadingGuide(fit, similarityNote);
	}

	private String createFit(Book book) {
		if (!"미분류".equals(book.category()) && !book.keywords().isEmpty()) {
			return book.category() + " 분야에서 " + book.keywords().getFirst() + " 키워드를 기준으로 다음 읽을 책을 고르는 사용자에게 맞습니다.";
		}
		if (!"미분류".equals(book.category())) {
			return book.category() + " 분야의 교양 도서를 찾는 사용자에게 맞습니다.";
		}
		return null;
	}

	private String createSimilarityNote(Book book, List<Book> similarBooks) {
		if (similarBooks.isEmpty()) {
			return null;
		}
		Book similarBook = similarBooks.getFirst();
		long sharedKeywordCount = similarBook.keywords().stream()
				.filter(book.keywords()::contains)
				.count();
		if (sharedKeywordCount > 0) {
			return "비슷한 책과 일부 키워드를 공유해 함께 비교해 볼 수 있습니다.";
		}
		if (book.category().equals(similarBook.category())) {
			return "비슷한 책과 같은 분야에 속하지만 키워드 구성은 다를 수 있습니다.";
		}
		return null;
	}

	private boolean isSaved(long userId, long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions save_interactions
				LEFT JOIN user_book_interactions later_unsave
					ON later_unsave.user_id = save_interactions.user_id
					AND later_unsave.book_id = save_interactions.book_id
					AND later_unsave.interaction_type = 'UNSAVE'
					AND (
						later_unsave.created_at > save_interactions.created_at
						OR (
							later_unsave.created_at = save_interactions.created_at
							AND later_unsave.id > save_interactions.id
						)
					)
				WHERE save_interactions.user_id = ?
					AND save_interactions.book_id = ?
					AND save_interactions.interaction_type = 'SAVE'
					AND later_unsave.id IS NULL
				""", Integer.class, userId, bookId);
		return count != null && count > 0;
	}

	private boolean isRead(long userId, long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions
				WHERE user_id = ?
					AND book_id = ?
					AND interaction_type = 'READ'
				""", Integer.class, userId, bookId);
		return count != null && count > 0;
	}

	private boolean isDismissed(long userId, long bookId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM user_book_interactions
				WHERE user_id = ?
					AND book_id = ?
					AND interaction_type = 'DISMISS'
				""", Integer.class, userId, bookId);
		return count != null && count > 0;
	}

	private List<Book> findSimilarBooks(Book book) {
		if ("미분류".equals(book.category())) {
			return List.of();
		}

		Set<String> keywords = new HashSet<>(book.keywords());
		return bookRepository.findByCategoriesNameAndGeneralEligibleTrueAndIdNot(
						book.category(),
						book.numericId(),
						pageById(SIMILAR_BOOK_CANDIDATE_LIMIT)
				).stream()
				.sorted((first, second) -> {
					int keywordComparison = Integer.compare(sharedKeywordCount(second, keywords), sharedKeywordCount(first, keywords));
					if (keywordComparison != 0) {
						return keywordComparison;
					}
					return Long.compare(second.numericId(), first.numericId());
				})
				.limit(SIMILAR_BOOK_LIMIT)
				.toList();
	}

	private int sharedKeywordCount(Book book, Set<String> keywords) {
		if (keywords.isEmpty()) {
			return 0;
		}

		return (int) book.keywords().stream()
				.filter(keywords::contains)
				.count();
	}

	private HomeResponse createHomeResponse(boolean personalized, Optional<BookSummaryResponse> recommendation) {
		return new HomeResponse(
				personalized,
				recommendation.orElse(null),
				findRanking("전체", "weekly", 10).stream().map(BookSummaryResponse::from).toList(),
				bookRankingSnapshotRepository.findLatestTrending(normalizePeriod("weekly"), page(10)).stream()
						.map(BookSummaryResponse::from)
						.toList(),
				getCategories().stream()
						.map(category -> new CategoryRankingResponse(
								category,
								findRanking(category, "weekly", 5).stream().map(BookSummaryResponse::from).toList()))
						.toList()
		);
	}

	private Optional<Book> getDefaultRecommendation() {
		return findRanking("전체", "weekly", 1).stream()
				.map(BookRankingSnapshot::book)
				.findFirst();
	}

	private Optional<PersonalizedRecommendation> getPersonalRecommendation(long userId) {
		Set<String> interestCategories = getInterestCategories(userId);
		List<ReadingPurpose> readingPurposes = getReadingPurposes(userId);
		List<Book> readBooks = getInteractedBooks(userId, "READ");
		List<Book> savedBooks = getSavedBooks(userId);
		Set<Long> excludedBookIds = getExcludedBookIds(userId);

		Set<String> readCategories = categoriesOf(readBooks);
		Set<String> savedCategories = categoriesOf(savedBooks);
		Set<String> readKeywords = keywordsOf(readBooks);
		Set<String> savedKeywords = keywordsOf(savedBooks);

		return bookRepository.findByGeneralEligibleTrue(pageById(PERSONAL_RECOMMENDATION_CANDIDATE_LIMIT)).stream()
				.filter(book -> !excludedBookIds.contains(book.numericId()))
				.map(book -> scorePersonalizedRecommendation(
						book,
						interestCategories,
						readingPurposes,
						readCategories,
						savedCategories,
						readKeywords,
						savedKeywords
				))
				.flatMap(Optional::stream)
				.max(Comparator.comparingInt(PersonalizedRecommendation::score)
						.thenComparing(recommendation -> recommendation.book().numericId()));
	}

	private Optional<PersonalizedRecommendation> scorePersonalizedRecommendation(
			Book book,
			Set<String> interestCategories,
			List<ReadingPurpose> readingPurposes,
			Set<String> readCategories,
			Set<String> savedCategories,
			Set<String> readKeywords,
			Set<String> savedKeywords
	) {
		String category = book.category();
		List<String> keywords = book.keywords();
		int score = 0;

		if (interestCategories.contains(category)) {
			score += 50;
		}
		if (readCategories.contains(category)) {
			score += 25;
		}
		if (savedCategories.contains(category)) {
			score += 20;
		}
		if (matchesPurposeCategory(category, readingPurposes)) {
			score += 20;
		}

		int sharedReadKeywords = sharedKeywordCount(keywords, readKeywords);
		int sharedSavedKeywords = sharedKeywordCount(keywords, savedKeywords);
		int sharedPurposeKeywords = sharedPurposeKeywordCount(keywords, readingPurposes);
		score += sharedReadKeywords * 10;
		score += sharedSavedKeywords * 8;
		score += sharedPurposeKeywords * 8;

		if (score <= 0) {
			return Optional.empty();
		}
		return Optional.of(new PersonalizedRecommendation(book, recommendationReason(book, interestCategories, readCategories,
				savedCategories, readKeywords, savedKeywords, readingPurposes), score));
	}

	private String recommendationReason(
			Book book,
			Set<String> interestCategories,
			Set<String> readCategories,
			Set<String> savedCategories,
			Set<String> readKeywords,
			Set<String> savedKeywords,
			List<ReadingPurpose> readingPurposes
	) {
		String category = book.category();
		if (interestCategories.contains(category)) {
			Optional<String> sharedKeyword = firstSharedKeyword(book.keywords(), readKeywords)
					.or(() -> firstSharedKeyword(book.keywords(), savedKeywords));
			return sharedKeyword
					.map(keyword -> "관심 분야로 선택한 " + category + " 분야와 맞고, " + keyword + " 키워드를 함께 가진 책입니다.")
					.orElse("관심 분야로 선택한 " + category + " 분야의 교양 도서입니다.");
		}

		Optional<String> readKeyword = firstSharedKeyword(book.keywords(), readKeywords);
		if (readKeyword.isPresent()) {
			return "읽은 책과 " + readKeyword.get() + " 키워드를 공유해 다음 독서 후보로 추천합니다.";
		}

		Optional<String> savedKeyword = firstSharedKeyword(book.keywords(), savedKeywords);
		if (savedKeyword.isPresent()) {
			return "저장한 책과 " + savedKeyword.get() + " 키워드를 공유합니다.";
		}
		if (savedCategories.contains(category)) {
			return "저장한 책과 비슷한 " + category + " 분야의 다음 후보입니다.";
		}
		if (readCategories.contains(category)) {
			return "읽은 책과 비슷한 " + category + " 분야의 교양 도서입니다.";
		}

		Optional<ReadingPurpose> categoryPurpose = firstPurposeByCategory(category, readingPurposes);
		if (categoryPurpose.isPresent()) {
			return categoryPurpose.get().label() + " 목적에 맞는 " + category + " 분야의 교양 도서입니다.";
		}

		Optional<String> purposeKeyword = firstSharedPurposeKeyword(book.keywords(), readingPurposes);
		if (purposeKeyword.isPresent()) {
			return purposeKeyword.get() + " 키워드가 선택한 독서 목적과 맞아 추천합니다.";
		}

		return FALLBACK_RECOMMENDATION_REASON;
	}

	private void saveRecommendation(long userId, PersonalizedRecommendation recommendation) {
		int updated = jdbcTemplate.update("""
				UPDATE recommendations
				SET reason = ?,
					score = ?
				WHERE user_id = ?
					AND book_id = ?
					AND recommendation_type = 'CONTENT_BASED'
				""", recommendation.reason(), recommendation.score(), userId, recommendation.book().numericId());
		if (updated > 0) {
			return;
		}
		jdbcTemplate.update("""
				INSERT INTO recommendations (user_id, book_id, recommendation_type, reason, score, created_at)
				VALUES (?, ?, 'CONTENT_BASED', ?, ?, CURRENT_TIMESTAMP)
				ON DUPLICATE KEY UPDATE
					reason = VALUES(reason),
					score = VALUES(score)
				""", userId, recommendation.book().numericId(), recommendation.reason(), recommendation.score());
	}

	private Set<String> getInterestCategories(long userId) {
		return new HashSet<>(jdbcTemplate.queryForList("""
				SELECT c.name
				FROM user_interest_categories uic
				JOIN categories c ON c.id = uic.category_id
				WHERE uic.user_id = ?
					AND c.is_active = TRUE
				""", String.class, userId));
	}

	private List<ReadingPurpose> getReadingPurposes(long userId) {
		return jdbcTemplate.queryForList("""
				SELECT purpose_code
				FROM user_reading_purposes
				WHERE user_id = ?
				ORDER BY created_at ASC, purpose_code ASC
				""", String.class, userId).stream()
				.map(ReadingPurpose::fromCode)
				.flatMap(Optional::stream)
				.toList();
	}

	private List<Book> getInteractedBooks(long userId, String interactionType) {
		List<Long> bookIds = jdbcTemplate.queryForList("""
				SELECT DISTINCT book_id
				FROM user_book_interactions
				WHERE user_id = ?
					AND interaction_type = ?
				""", Long.class, userId, interactionType);
		return bookRepository.findAllById(bookIds);
	}

	private List<Book> getSavedBooks(long userId) {
		List<Long> bookIds = jdbcTemplate.queryForList("""
				SELECT DISTINCT save_interactions.book_id
				FROM user_book_interactions save_interactions
				LEFT JOIN user_book_interactions later_unsave
					ON later_unsave.user_id = save_interactions.user_id
					AND later_unsave.book_id = save_interactions.book_id
					AND later_unsave.interaction_type = 'UNSAVE'
					AND (
						later_unsave.created_at > save_interactions.created_at
						OR (
							later_unsave.created_at = save_interactions.created_at
							AND later_unsave.id > save_interactions.id
						)
					)
				WHERE save_interactions.user_id = ?
					AND save_interactions.interaction_type = 'SAVE'
					AND later_unsave.id IS NULL
				""", Long.class, userId);
		return bookRepository.findAllById(bookIds);
	}

	private Set<Long> getExcludedBookIds(long userId) {
		return new HashSet<>(jdbcTemplate.queryForList("""
				SELECT DISTINCT book_id
				FROM user_book_interactions
				WHERE user_id = ?
					AND interaction_type IN ('READ', 'DISMISS')
				""", Long.class, userId));
	}

	private Set<String> categoriesOf(List<Book> books) {
		return books.stream()
				.map(Book::category)
				.collect(java.util.stream.Collectors.toSet());
	}

	private Set<String> keywordsOf(List<Book> books) {
		return books.stream()
				.flatMap(book -> book.keywords().stream())
				.collect(java.util.stream.Collectors.toSet());
	}

	private int sharedKeywordCount(List<String> keywords, Set<String> referenceKeywords) {
		if (referenceKeywords.isEmpty()) {
			return 0;
		}
		return (int) keywords.stream()
				.filter(referenceKeywords::contains)
				.count();
	}

	private Optional<String> firstSharedKeyword(List<String> keywords, Set<String> referenceKeywords) {
		return keywords.stream()
				.filter(referenceKeywords::contains)
				.findFirst();
	}

	private boolean matchesPurposeCategory(String category, List<ReadingPurpose> readingPurposes) {
		return readingPurposes.stream()
				.anyMatch(purpose -> purpose.categoryNames().contains(category));
	}

	private int sharedPurposeKeywordCount(List<String> keywords, List<ReadingPurpose> readingPurposes) {
		Set<String> purposeKeywords = readingPurposes.stream()
				.flatMap(purpose -> purpose.keywords().stream())
				.collect(java.util.stream.Collectors.toSet());
		return sharedKeywordCount(keywords, purposeKeywords);
	}

	private Optional<ReadingPurpose> firstPurposeByCategory(String category, List<ReadingPurpose> readingPurposes) {
		return readingPurposes.stream()
				.filter(purpose -> purpose.categoryNames().contains(category))
				.findFirst();
	}

	private Optional<String> firstSharedPurposeKeyword(List<String> keywords, List<ReadingPurpose> readingPurposes) {
		Set<String> purposeKeywords = readingPurposes.stream()
				.flatMap(purpose -> purpose.keywords().stream())
				.collect(java.util.stream.Collectors.toSet());
		return firstSharedKeyword(keywords, purposeKeywords);
	}

	private List<BookRankingSnapshot> findRanking(String category, String period, int limit) {
		String normalizedPeriod = normalizePeriod(period);
		if ("전체".equals(category)) {
			return bookRankingSnapshotRepository.findLatestOverallRankings(normalizedPeriod, page(limit));
		}
		return bookRankingSnapshotRepository.findLatestCategoryRankings(category, normalizedPeriod, page(limit));
	}

	private PageRequest page(int limit) {
		return PageRequest.of(0, limit);
	}

	private PageRequest pageById(int limit) {
		return PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
	}

	private int normalizeLimit(int limit) {
		if (limit <= 0) {
			return 10;
		}
		return Math.min(limit, 50);
	}

	private int normalizeSearchLimit(int limit) {
		if (limit <= 0) {
			return SEARCH_DEFAULT_LIMIT;
		}
		return Math.min(limit, SEARCH_MAX_LIMIT);
	}

	private int normalizeKeywordTrendLimit(int limit) {
		if (limit <= 0) {
			return KEYWORD_TREND_DEFAULT_LIMIT;
		}
		return Math.min(limit, KEYWORD_TREND_MAX_LIMIT);
	}

	private int normalizeKeywordTrendBookLimit(int limit) {
		if (limit <= 0) {
			return KEYWORD_TREND_BOOK_DEFAULT_LIMIT;
		}
		return Math.min(limit, KEYWORD_TREND_BOOK_MAX_LIMIT);
	}

	private String formatTrendScore(BigDecimal score) {
		return "%+.0f%%".formatted(score == null ? 0 : score.doubleValue());
	}

	private String normalizeSearchQuery(String query) {
		if (query == null || query.trim().length() < SEARCH_MIN_QUERY_LENGTH) {
			throw new BookRequestException("Search query must be at least 2 characters.");
		}
		return query.trim();
	}

	private void validateCategory(String category, boolean allowAll) {
		if (allowAll && "전체".equals(category)) {
			return;
		}

		if (!categoryRepository.existsByName(category)) {
			throw new BookRequestException("Unsupported category.");
		}
	}

	private void validatePeriod(String period) {
		if (!PERIODS.contains(period)) {
			throw new BookRequestException("Unsupported period.");
		}
	}

	private String normalizePeriod(String period) {
		return period.toUpperCase();
	}

	private Long parseBookId(String bookId) {
		try {
			return Long.parseLong(bookId);
		} catch (NumberFormatException exception) {
			throw new BookNotFoundException("Book not found.");
		}
	}

	public static class BookNotFoundException extends RuntimeException {

		public BookNotFoundException(String message) {
			super(message);
		}
	}

	public static class BookRequestException extends RuntimeException {

		public BookRequestException(String message) {
			super(message);
		}
	}

	private record PersonalizedRecommendation(Book book, String reason, int score) {
	}

	private record KeywordTrend(String keyword, int bookCount, BigDecimal trendScore) {
	}
}
