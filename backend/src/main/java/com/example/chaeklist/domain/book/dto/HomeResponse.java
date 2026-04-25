package com.example.chaeklist.domain.book.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

	@Schema(description = "홈 화면 응답")
public record HomeResponse(
		@Schema(description = "개인화 홈 여부", example = "false")
		boolean personalized,
		@Schema(description = "오늘의 추천")
		@Nullable
		BookSummaryResponse todayRecommendation,
		@Schema(description = "현재 인기 책")
		List<BookSummaryResponse> popularBooks,
		@Schema(description = "급상승 책")
		List<BookSummaryResponse> trendingBooks,
		@Schema(description = "카테고리별 랭킹")
		List<CategoryRankingResponse> categoryRankings
) {
}
