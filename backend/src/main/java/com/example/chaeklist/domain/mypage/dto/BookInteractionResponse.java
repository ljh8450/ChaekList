package com.example.chaeklist.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책 행동 상태 응답")
public record BookInteractionResponse(
		@Schema(description = "책 ID", example = "301")
		String bookId,
		@Schema(description = "현재 사용자의 저장 여부", example = "true")
		boolean saved,
		@Schema(description = "현재 사용자의 읽음 여부", example = "false")
		boolean read,
		@Schema(description = "현재 사용자의 관심 없음 여부", example = "false")
		boolean dismissed
) {
}
