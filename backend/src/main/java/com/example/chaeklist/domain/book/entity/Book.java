package com.example.chaeklist.domain.book.entity;

import java.util.List;

public record Book(
		String id,
		String title,
		String author,
		String category,
		String tag,
		String summary,
		String recommendationReason,
		String views,
		int saves,
		int growthRate,
		List<String> keywords
) {
}
