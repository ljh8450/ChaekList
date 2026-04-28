package com.example.chaeklist.domain.book.dto;

import java.time.LocalDate;

import com.example.chaeklist.domain.book.entity.Book;
import com.example.chaeklist.domain.book.entity.BookRankingSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

@Schema(description = "책 요약 응답")
public record BookSummaryResponse(
		@Schema(description = "책 ID", example = "1")
		String id,
		@Schema(description = "랭킹 순위. 랭킹 응답이 아니면 null입니다.", example = "1", nullable = true)
		@Nullable
		Integer rankPosition,
		@Schema(description = "랭킹 기간. 랭킹 응답이 아니면 null입니다.", example = "WEEKLY", nullable = true)
		@Nullable
		String rankingPeriod,
		@Schema(description = "랭킹 기준일. 랭킹 응답이 아니면 null입니다.", example = "2026-04-20", nullable = true)
		@Nullable
		LocalDate rankDate,
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
		return from(book, book.recommendationReason());
	}

	public static BookSummaryResponse from(Book book, String recommendationReason) {
		return new BookSummaryResponse(
				book.id(),
				null,
				null,
				null,
				book.title(),
				book.author(),
				book.coverImageUrl(),
				book.category(),
				book.tag(),
				book.views(),
				book.saves(),
				"+" + book.growthRate() + "%",
				recommendationReason
		);
	}

	public static BookSummaryResponse from(BookRankingSnapshot snapshot) {
		Book book = snapshot.book();
		return new BookSummaryResponse(
				book.id(),
				snapshot.rankPosition(),
				snapshot.rankingPeriod(),
				snapshot.rankDate(),
				book.title(),
				book.author(),
				book.coverImageUrl(),
				book.category(),
				book.tag(),
				formatCount(snapshot.viewCount()),
				snapshot.saveCount(),
				formatGrowthRate(snapshot),
				"랭킹 지표와 교양 필터링 기준을 반영한 책입니다."
		);
	}

	private static String formatCount(int count) {
		if (count >= 1000) {
			return String.format("%.1fk", count / 1000.0);
		}
		return String.valueOf(count);
	}

	private static String formatGrowthRate(BookRankingSnapshot snapshot) {
		return "%+.0f%%".formatted(snapshot.recentGrowthRate().doubleValue());
	}
}
