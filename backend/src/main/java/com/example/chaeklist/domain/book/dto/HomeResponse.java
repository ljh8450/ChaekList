package com.example.chaeklist.domain.book.dto;

import java.util.List;

public record HomeResponse(
		boolean personalized,
		BookSummaryResponse todayRecommendation,
		List<BookSummaryResponse> popularBooks,
		List<BookSummaryResponse> trendingBooks,
		List<CategoryRankingResponse> categoryRankings
) {
}
