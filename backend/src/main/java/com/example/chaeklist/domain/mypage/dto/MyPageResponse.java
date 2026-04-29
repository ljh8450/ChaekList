package com.example.chaeklist.domain.mypage.dto;

import java.util.List;

import com.example.chaeklist.domain.auth.dto.AuthUserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 응답")
public record MyPageResponse(
		@Schema(description = "사용자 정보")
		AuthUserResponse user,
		@Schema(description = "관심 분야")
		List<MyPageInterestResponse> interests,
		@Schema(description = "독서 목적")
		List<ReadingPurposeResponse> readingPurposes,
		@Schema(description = "읽은 책")
		List<MyPageBookResponse> readBooks,
		@Schema(description = "저장한 책")
		List<MyPageBookResponse> savedBooks,
		@Schema(description = "추천 히스토리")
		List<MyPageRecommendationResponse> recommendationHistory,
		@Schema(description = "독서 성장")
		ReadingGrowthResponse readingGrowth
) {
}
