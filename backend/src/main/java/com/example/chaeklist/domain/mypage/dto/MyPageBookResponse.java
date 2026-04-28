package com.example.chaeklist.domain.mypage.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 도서 응답")
public record MyPageBookResponse(
		@Schema(description = "책 ID", example = "101")
		String id,
		@Schema(description = "제목", example = "느리게 읽는 법")
		String title,
		@Schema(description = "저자", example = "문서윤")
		String author,
		@Schema(description = "책 표지 이미지 URL", example = "https://example.com/book-cover.jpg", nullable = true)
		String imageUrl,
		@Schema(description = "카테고리", example = "인문")
		String category,
		@Schema(description = "교양 필터 또는 상태 태그", example = "교양 필터 통과")
		String tag,
		@Schema(description = "조회수 표시값", example = "12")
		String views,
		@Schema(description = "저장 수", example = "3")
		int saves,
		@Schema(description = "추천 이유 또는 설명")
		String recommendationReason,
		@Schema(description = "사용자 상호작용 시각")
		LocalDateTime interactedAt
) {
}
