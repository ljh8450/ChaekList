package com.example.chaeklist.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책 행동 기록 요청")
public record BookInteractionRequest(
		@Schema(description = "행동 타입", example = "SAVE", allowableValues = {"SAVE", "UNSAVE", "READ", "DISMISS"})
		String type
) {
}
