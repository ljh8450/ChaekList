package com.example.chaeklist.domain.mypage.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 추천 히스토리 응답")
public record MyPageRecommendationResponse(
		@Schema(description = "추천 ID", example = "1")
		long id,
		@Schema(description = "책 ID", example = "101")
		String bookId,
		@Schema(description = "책 제목", example = "느리게 읽는 법")
		String title,
		@Schema(description = "추천 이유")
		String reason,
		@Schema(description = "추천 출처", example = "CONTENT_BASED")
		String source,
		@Schema(description = "추천 점수", example = "92")
		int score,
		@Schema(description = "추천 생성 시각")
		LocalDateTime createdAt
) {
}
