package com.example.chaeklist.domain.book.dto;

import com.example.chaeklist.domain.book.entity.Book;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책 요약 응답")
public record BookSummaryResponse(
		@Schema(description = "책 ID", example = "1")
		String id,
		@Schema(description = "제목", example = "느리게 읽는 힘")
		String title,
		@Schema(description = "저자", example = "문서윤")
		String author,
		@Schema(description = "카테고리", example = "인문")
		String category,
		@Schema(description = "책 태그", example = "교양 필터 통과")
		String tag,
		@Schema(description = "조회수 표시값", example = "12.4k")
		String views,
		@Schema(description = "저장 또는 찜 수", example = "842")
		int saves,
		@Schema(description = "최근 상승률", example = "+18%")
		String growthRate,
		@Schema(description = "추천 이유", example = "최근 인문 분야에서 저장 수가 빠르게 늘고 있습니다.")
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
