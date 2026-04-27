package com.example.chaeklist.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 관심 분야 응답")
public record MyPageInterestResponse(
		@Schema(description = "카테고리 ID", example = "1")
		Long id,
		@Schema(description = "카테고리명", example = "인문")
		String label,
		@Schema(description = "관심 분야 설명")
		String description,
		@Schema(description = "사용자 행동 기반 관심 점수", example = "80")
		int score
) {
}
