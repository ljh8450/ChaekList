package com.example.chaeklist.domain.mypage.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "독서 성장 응답")
public record ReadingGrowthResponse(
		@Schema(description = "내부 계산용 성장 레벨 숫자", example = "2")
		int level,
		@Schema(description = "다음 성장 단계까지 진행률", example = "45")
		int progressPercent,
		@Schema(description = "독서 성장 요약")
		String summary,
		@Schema(description = "이번 달 읽은 책 수", example = "3")
		int monthlyReadCount,
		@Schema(description = "저장 후 읽음 전환 수", example = "1")
		int savedToReadCount,
		@Schema(description = "읽은 책 카테고리 다양성 수", example = "2")
		int categoryDiversityCount,
		@Schema(description = "추천받은 책 저장/읽음 전환 수", example = "1")
		int recommendationConversionCount,
		@Schema(description = "대표 배지")
		Badge primaryBadge,
		@Schema(description = "획득 조건을 충족한 배지")
		List<Badge> badges
) {

	public record Badge(
			@Schema(description = "배지 코드", example = "FIRST_READ")
			String code,
			@Schema(description = "배지 이름", example = "첫 독서 기록")
			String label,
			@Schema(description = "배지 설명")
			String description
	) {
	}
}
