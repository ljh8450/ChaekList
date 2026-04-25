package com.example.chaeklist.domain.book.dto;

import com.example.chaeklist.domain.book.entity.Book;

public record BookSummaryResponse(
		String id,
		String title,
		String author,
		String category,
		String tag,
		String views,
		int saves,
		String growthRate,
		String recommendationReason
) {

	public static BookSummaryResponse from(Book book) {
		return new BookSummaryResponse(
				book.id(),
				book.title(),
				book.author(),
				book.category(),
				book.tag(),
				book.views(),
				book.saves(),
				"+" + book.growthRate() + "%",
				book.recommendationReason()
		);
	}
}
