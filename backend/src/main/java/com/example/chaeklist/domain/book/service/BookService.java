package com.example.chaeklist.domain.book.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.chaeklist.domain.book.dto.BookDetailResponse;
import com.example.chaeklist.domain.book.dto.BookSummaryResponse;
import com.example.chaeklist.domain.book.dto.CategoryRankingResponse;
import com.example.chaeklist.domain.book.dto.HomeResponse;
import com.example.chaeklist.domain.book.entity.Book;
import com.example.chaeklist.domain.book.entity.BookRankingSnapshot;
import com.example.chaeklist.domain.book.repository.BookRankingSnapshotRepository;
import com.example.chaeklist.domain.book.repository.BookRepository;
import com.example.chaeklist.domain.book.repository.CategoryRepository;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookService {

	private static final Set<String> PERIODS = Set.of("daily", "weekly", "monthly");
	private static final int SIMILAR_BOOK_LIMIT = 3;
	private static final int SIMILAR_BOOK_CANDIDATE_LIMIT = 50;
	private static final int PERSONAL_RECOMMENDATION_CANDIDATE_LIMIT = 50;
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

	public HomeResponse getPersonalHome(AuthenticatedUser user) {
		Optional<BookSummaryResponse> recommendation = getPersonalRecommendation(user.id())
				.map(personalizedRecommendation -> BookSummaryResponse.from(
						personalizedRecommendation.book(),
						personalizedRecommendation.reason()
				))
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
		return BookDetailResponse.from(book, similarBooks);
	}

	public BookDetailResponse getBookDetail(String bookId, AuthenticatedUser user) {
		Long id = parseBookId(bookId);
		Book book = bookRepository.findByIdAndGeneralEligibleTrue(id)
				.orElseThrow(() -> new BookNotFoundException("Book not found."));
		List<Book> similarBooks = findSimilarBooks(book);
		return BookDetailResponse.from(book, similarBooks, isSaved(user.id(), id), isRead(user.id(), id), isDismissed(user.id(), id));
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

		int sharedReadKeywords = sharedKeywordCount(keywords, readKeywords);
		int sharedSavedKeywords = sharedKeywordCount(keywords, savedKeywords);
		score += sharedReadKeywords * 10;
		score += sharedSavedKeywords * 8;

		if (score <= 0) {
			return Optional.empty();
		}
		return Optional.of(new PersonalizedRecommendation(book, recommendationReason(book, interestCategories, readCategories,
				savedCategories, readKeywords, savedKeywords), score));
	}

	private String recommendationReason(
			Book book,
			Set<String> interestCategories,
			Set<String> readCategories,
			Set<String> savedCategories,
			Set<String> readKeywords,
			Set<String> savedKeywords
	) {
		String category = book.category();
		if (interestCategories.contains(category)) {
			return "관심 분야로 선택한 " + category + " 분야의 교양 도서입니다.";
		}

		Optional<String> readKeyword = firstSharedKeyword(book.keywords(), readKeywords);
		if (readKeyword.isPresent()) {
			return "읽은 책과 " + readKeyword.get() + " 키워드를 공유합니다.";
		}

		if (savedCategories.contains(category)) {
			return "저장한 책과 비슷한 " + category + " 분야의 다음 후보입니다.";
		}
		if (readCategories.contains(category)) {
			return "읽은 책과 비슷한 " + category + " 분야의 교양 도서입니다.";
		}

		Optional<String> savedKeyword = firstSharedKeyword(book.keywords(), savedKeywords);
		return savedKeyword
				.map(keyword -> "저장한 책과 " + keyword + " 키워드를 공유합니다.")
				.orElse(FALLBACK_RECOMMENDATION_REASON);
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
}
