package com.example.chaeklist.domain.book.service;

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
		return createHomeResponse(false, getDefaultRecommendation());
	}

	public HomeResponse getPersonalHome(AuthenticatedUser user) {
		Optional<Book> recommendation = getDefaultRecommendation();
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

	private HomeResponse createHomeResponse(boolean personalized, Optional<Book> recommendation) {
		return new HomeResponse(
				personalized,
				recommendation.map(BookSummaryResponse::from).orElse(null),
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
}
