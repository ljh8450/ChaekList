package com.example.chaeklist.domain.mypage.dto;

import com.example.chaeklist.domain.mypage.model.ReadingPurpose;

public record ReadingPurposeResponse(
		String code,
		String label,
		String description
) {

	public static ReadingPurposeResponse from(ReadingPurpose purpose) {
		return new ReadingPurposeResponse(purpose.code(), purpose.label(), purpose.description());
	}
}
