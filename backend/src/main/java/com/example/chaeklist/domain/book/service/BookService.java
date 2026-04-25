package com.example.chaeklist.domain.book.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.chaeklist.domain.book.dto.BookDetailResponse;
import com.example.chaeklist.domain.book.dto.BookSummaryResponse;
import com.example.chaeklist.domain.book.dto.CategoryRankingResponse;
import com.example.chaeklist.domain.book.dto.HomeResponse;
import com.example.chaeklist.domain.book.entity.Book;
import com.example.chaeklist.domain.book.repository.BookRepository;
import com.example.chaeklist.domain.book.repository.CategoryRepository;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class BookService {

	private static final Set<String> PERIODS = Set.of("daily", "weekly", "monthly");

	private final BookRepository bookRepository;
	private final CategoryRepository categoryRepository;

	public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
		this.bookRepository = bookRepository;
		this.categoryRepository = categoryRepository;
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
		return findRanking(category, normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public List<BookSummaryResponse> getTrending(int limit) {
		return bookRepository.findByGeneralEligibleTrue(pageById(normalizeLimit(limit))).stream()
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
		return findRanking(category, normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public BookDetailResponse getBookDetail(String bookId) {
		Long id = parseBookId(bookId);
		Book book = bookRepository.findByIdAndGeneralEligibleTrue(id)
				.orElseThrow(() -> new BookNotFoundException("Book not found."));
		List<Book> similarBooks = "미분류".equals(book.category())
				? List.of()
				: bookRepository.findByCategoriesNameAndGeneralEligibleTrueAndIdNot(book.category(), book.numericId(), pageById(3));
		return BookDetailResponse.from(book, similarBooks);
	}

	private HomeResponse createHomeResponse(boolean personalized, Optional<Book> recommendation) {
		return new HomeResponse(
				personalized,
				recommendation.map(BookSummaryResponse::from).orElse(null),
				findRanking("전체", 10).stream().map(BookSummaryResponse::from).toList(),
				bookRepository.findByGeneralEligibleTrue(pageById(10)).stream().map(BookSummaryResponse::from).toList(),
				getCategories().stream()
						.map(category -> new CategoryRankingResponse(
								category,
								findRanking(category, 5).stream().map(BookSummaryResponse::from).toList()))
						.toList()
		);
	}

	private Optional<Book> getDefaultRecommendation() {
		return findRanking("전체", 1).stream().findFirst();
	}

	private List<Book> findRanking(String category, int limit) {
		if ("전체".equals(category)) {
			return bookRepository.findByGeneralEligibleTrue(pageById(limit));
		}
		return bookRepository.findByCategoriesNameAndGeneralEligibleTrue(category, pageById(limit));
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
