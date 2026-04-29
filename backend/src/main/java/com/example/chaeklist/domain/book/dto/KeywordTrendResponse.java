package com.example.chaeklist.domain.book.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "키워드 기반 트렌드 응답")
public record KeywordTrendResponse(
		@Schema(description = "트렌드 키워드", example = "투자")
		String keyword,
		@Schema(description = "키워드에 연결된 교양 도서 수", example = "12")
		int bookCount,
		@Schema(description = "키워드 트렌드 점수 표시값", example = "+18%")
		String trendScore,
		@Schema(description = "키워드와 연결된 대표 도서")
		List<BookSummaryResponse> books
) {
}
