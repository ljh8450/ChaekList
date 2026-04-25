package com.example.chaeklist.domain.book.dto;

import java.util.List;

import com.example.chaeklist.domain.book.entity.Book;

public record BookDetailResponse(
		String id,
		String title,
		String author,
		String category,
		String tag,
		String summary,
		String recommendationReason,
		String views,
		int saves,
		String growthRate,
		List<String> keywords,
		List<BookSummaryResponse> similarBooks
) {

	public static BookDetailResponse from(Book book, List<Book> similarBooks) {
		return new BookDetailResponse(
				book.id(),
				book.title(),
				book.author(),
				book.category(),
				book.tag(),
				book.summary(),
				book.recommendationReason(),
				book.views(),
				book.saves(),
				"+" + book.growthRate() + "%",
				book.keywords(),
				similarBooks.stream().map(BookSummaryResponse::from).toList()
		);
	}
}
