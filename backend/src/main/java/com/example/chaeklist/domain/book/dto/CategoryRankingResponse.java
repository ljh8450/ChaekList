package com.example.chaeklist.domain.book.dto;

import java.util.List;

public record CategoryRankingResponse(
		String category,
		List<BookSummaryResponse> books
) {
}
