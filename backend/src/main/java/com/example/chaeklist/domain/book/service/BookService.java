package com.example.chaeklist.domain.book.service;

import java.util.List;
import java.util.Set;

import com.example.chaeklist.domain.book.dto.BookDetailResponse;
import com.example.chaeklist.domain.book.dto.BookSummaryResponse;
import com.example.chaeklist.domain.book.dto.CategoryRankingResponse;
import com.example.chaeklist.domain.book.dto.HomeResponse;
import com.example.chaeklist.domain.book.entity.Book;
import com.example.chaeklist.domain.book.repository.BookRepository;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class BookService {

	private static final List<String> CATEGORIES = List.of("인문", "경제", "자기계발", "소설", "에세이");
	private static final Set<String> PERIODS = Set.of("daily", "weekly", "monthly");

	private final BookRepository bookRepository;

	public BookService(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	public HomeResponse getPublicHome() {
		return createHomeResponse(false, getDefaultRecommendation());
	}

	public HomeResponse getPersonalHome(AuthenticatedUser user) {
		Book recommendation = bookRepository.findByCategory("경제").stream()
				.findFirst()
				.orElseGet(this::getDefaultRecommendation);
		return createHomeResponse(true, recommendation);
	}

	public List<BookSummaryResponse> getRankings(String category, String period, int limit) {
		validateCategory(category, true);
		validatePeriod(period);
		return bookRepository.findRanking(category, normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public List<BookSummaryResponse> getTrending(int limit) {
		return bookRepository.findTrending(normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public List<String> getCategories() {
		return CATEGORIES;
	}

	public List<BookSummaryResponse> getCategoryRankings(String category, String period, int limit) {
		validateCategory(category, false);
		validatePeriod(period);
		return bookRepository.findRanking(category, normalizeLimit(limit)).stream()
				.map(BookSummaryResponse::from)
				.toList();
	}

	public BookDetailResponse getBookDetail(String bookId) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new BookNotFoundException("Book not found."));
		List<Book> similarBooks = bookRepository.findByCategory(book.category()).stream()
				.filter(item -> !item.id().equals(book.id()))
				.limit(3)
				.toList();
		return BookDetailResponse.from(book, similarBooks);
	}

	private HomeResponse createHomeResponse(boolean personalized, Book recommendation) {
		return new HomeResponse(
				personalized,
				BookSummaryResponse.from(recommendation),
				bookRepository.findRanking("전체", 10).stream().map(BookSummaryResponse::from).toList(),
				bookRepository.findTrending(10).stream().map(BookSummaryResponse::from).toList(),
				CATEGORIES.stream()
						.map(category -> new CategoryRankingResponse(
								category,
								bookRepository.findRanking(category, 5).stream().map(BookSummaryResponse::from).toList()))
						.toList()
		);
	}

	private Book getDefaultRecommendation() {
		return bookRepository.findRanking("전체", 1).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Book catalog is empty."));
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

		if (!CATEGORIES.contains(category)) {
			throw new BookRequestException("Unsupported category.");
		}
	}

	private void validatePeriod(String period) {
		if (!PERIODS.contains(period)) {
			throw new BookRequestException("Unsupported period.");
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
