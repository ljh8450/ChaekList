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
		return from(book, similarBooks, false, false, false);
	}

	public static BookDetailResponse from(Book book, List<Book> similarBooks, boolean saved, boolean read, boolean dismissed) {
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
				createFilterReport(book),
				createRecommendationEvidence(book),
				createReadingGuide(book, similarBooks),
				saved,
				read,
				dismissed
		);
	}

	private static FilterReport createFilterReport(Book book) {
		return new FilterReport(
				book.filterStatus(),
				book.tag(),
				"미분류".equals(book.category()) ? null : book.category(),
				book.keywords()
		);
	}

	private static List<RecommendationEvidence> createRecommendationEvidence(Book book) {
		List<String> keywords = book.keywords();
		java.util.ArrayList<RecommendationEvidence> evidence = new java.util.ArrayList<>();
		if (!"미분류".equals(book.category())) {
			evidence.add(new RecommendationEvidence(
					"CATEGORY",
					"관심 분야",
					book.category() + " 분야 책을 찾는 사용자에게 맞는 후보입니다."
			));
		}
		if (!keywords.isEmpty()) {
			evidence.add(new RecommendationEvidence(
					"KEYWORD",
					"공통 키워드",
					keywords.getFirst() + " 키워드를 중심으로 탐색할 수 있는 책입니다."
			));
		}
		if (book.generalEligible()) {
			evidence.add(new RecommendationEvidence(
					"FILTER",
					"교양 필터",
					book.tag() + " 기준으로 상세 후보에 포함되었습니다."
			));
		}
		return evidence.stream().limit(3).toList();
	}

	private static ReadingGuide createReadingGuide(Book book, List<Book> similarBooks) {
		String fit = createFit(book);
		String similarityNote = createSimilarityNote(book, similarBooks);
		if (fit == null && similarityNote == null) {
			return null;
		}
		return new ReadingGuide(fit, similarityNote);
	}

	private static String createFit(Book book) {
		if (!"미분류".equals(book.category()) && !book.keywords().isEmpty()) {
			return book.category() + " 분야에서 " + book.keywords().getFirst() + " 키워드를 기준으로 다음 읽을 책을 고르는 사용자에게 맞습니다.";
		}
		if (!"미분류".equals(book.category())) {
			return book.category() + " 분야의 교양 도서를 찾는 사용자에게 맞습니다.";
		}
		return null;
	}

	private static String createSimilarityNote(Book book, List<Book> similarBooks) {
		if (similarBooks.isEmpty()) {
			return null;
		}
		Book similarBook = similarBooks.getFirst();
		long sharedKeywordCount = similarBook.keywords().stream()
				.filter(book.keywords()::contains)
				.count();
		if (sharedKeywordCount > 0) {
			return "비슷한 책과 일부 키워드를 공유해 함께 비교해 볼 수 있습니다.";
		}
		if (book.category().equals(similarBook.category())) {
			return "비슷한 책과 같은 분야에 속하지만 키워드 구성은 다를 수 있습니다.";
		}
		return null;
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
			@Schema(description = "근거 제목", example = "관심 분야")
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
