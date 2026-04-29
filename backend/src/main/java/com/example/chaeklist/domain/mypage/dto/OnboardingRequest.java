package com.example.chaeklist.domain.mypage.dto;

import java.util.List;

public record OnboardingRequest(
		List<Long> categoryIds,
		List<Long> readBookIds,
		List<String> readingPurposeCodes
) {
}
