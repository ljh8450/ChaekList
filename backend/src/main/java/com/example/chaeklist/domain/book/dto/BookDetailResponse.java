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
		@Schema(description = "책 표지 이미지 URL", example = "https://example.com/book-cover.jpg", nullable = true)
		String imageUrl,
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
		@Schema(description = "교양 필터 리포트", nullable = true)
		FilterReport filterReport,
		@Schema(description = "추천 근거 목록")
		List<RecommendationEvidence> recommendationEvidence,
		@Schema(description = "읽을 책 결정 보조", nullable = true)
		ReadingGuide readingGuide,
		@Schema(description = "현재 사용자의 저장 여부", example = "true")
		boolean saved,
		@Schema(description = "현재 사용자의 읽음 여부", example = "false")
		boolean read,
		@Schema(description = "현재 사용자의 관심 없음 여부", example = "false")
		boolean dismissed
) {

	public static BookDetailResponse from(Book book, List<Book> similarBooks) {
		return from(book, similarBooks, null, List.of(), null, false, false, false);
	}

	public static BookDetailResponse from(Book book, List<Book> similarBooks, boolean saved, boolean read, boolean dismissed) {
		return from(book, similarBooks, null, List.of(), null, saved, read, dismissed);
	}

	public static BookDetailResponse from(
			Book book,
			List<Book> similarBooks,
			FilterReport filterReport,
			List<RecommendationEvidence> recommendationEvidence,
			ReadingGuide readingGuide,
			boolean saved,
			boolean read,
			boolean dismissed
	) {
		return new BookDetailResponse(
				book.id(),
				book.title(),
				book.author(),
				book.coverImageUrl(),
				book.category(),
				book.tag(),
				book.summary(),
				book.recommendationReason(),
				book.views(),
				book.saves(),
				"+" + book.growthRate() + "%",
				book.keywords(),
				similarBooks.stream().map(BookSummaryResponse::from).toList(),
				filterReport,
				recommendationEvidence,
				readingGuide,
				saved,
				read,
				dismissed
		);
	}

	@Schema(description = "교양 필터 리포트")
	public record FilterReport(
			@Schema(description = "교양 필터 상태", example = "INCLUDED")
			String status,
			@Schema(description = "교양서로 노출된 이유", example = "교양 필터 통과")
			String reason,
			@Schema(description = "대표 카테고리", example = "인문", nullable = true)
			String category,
			@Schema(description = "주요 키워드", example = "[\"독서\", \"사유\"]")
			List<String> keywords
	) {
	}

	@Schema(description = "추천 근거")
	public record RecommendationEvidence(
			@Schema(description = "근거 유형", example = "CATEGORY")
			String type,
			@Schema(description = "근거 제목", example = "대표 분야")
			String label,
			@Schema(description = "근거 설명")
			String description
	) {
	}

	@Schema(description = "읽을 책 결정 보조")
	public record ReadingGuide(
			@Schema(description = "어떤 사용자에게 맞는지", nullable = true)
			String fit,
			@Schema(description = "비슷한 책과 비교할 때 참고할 점", nullable = true)
			String similarityNote
	) {
	}
}
