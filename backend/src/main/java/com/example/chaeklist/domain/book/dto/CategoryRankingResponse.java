package com.example.chaeklist.domain.book.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리별 랭킹 응답")
public record CategoryRankingResponse(
		@Schema(description = "카테고리명", example = "경제")
		String category,
		@Schema(description = "해당 카테고리의 랭킹 책 목록")
		List<BookSummaryResponse> books
) {
}
