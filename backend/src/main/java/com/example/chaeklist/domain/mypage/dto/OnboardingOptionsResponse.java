package com.example.chaeklist.domain.mypage.dto;

import java.util.List;

public record OnboardingOptionsResponse(
		List<OnboardingCategoryOptionResponse> categories,
		List<OnboardingBookOptionResponse> books,
		List<ReadingPurposeResponse> readingPurposes
) {
}
