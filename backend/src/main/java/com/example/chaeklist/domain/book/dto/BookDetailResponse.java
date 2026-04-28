package com.example.chaeklist.domain.book.dto;

import java.util.List;

import com.example.chaeklist.domain.book.entity.Book;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책 상세 응답")
public record BookDetailResponse(
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
		@Schema(description = "책 요약")
		String summary,
		@Schema(description = "추천 이유")
		String recommendationReason,
		@Schema(description = "조회수 표시값", example = "12.4k")
		String views,
		@Schema(description = "저장 또는 찜 수", example = "842")
		int saves,
		@Schema(description = "최근 상승률", example = "+18%")
		String growthRate,
		@Schema(description = "키워드 목록", example = "[\"독서\", \"사유\", \"집중\"]")
		List<String> keywords,
		@Schema(description = "비슷한 책 목록")
		List<BookSummaryResponse> similarBooks,
		@Schema(description = "현재 사용자의 저장 여부", example = "true")
		boolean saved,
		@Schema(description = "현재 사용자의 읽음 여부", example = "false")
		boolean read
) {

	public static BookDetailResponse from(Book book, List<Book> similarBooks) {
		return from(book, similarBooks, false, false);
	}

	public static BookDetailResponse from(Book book, List<Book> similarBooks, boolean saved, boolean read) {
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
				similarBooks.stream().map(BookSummaryResponse::from).toList(),
				saved,
				read
		);
	}
}
