package com.example.chaeklist.domain.book.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책 표지 이미지 보강 결과")
public record BookImageEnrichmentResponse(
		@Schema(description = "보강 대상으로 조회한 책 수", example = "20")
		int processed,
		@Schema(description = "이미지 URL 저장 성공 수", example = "15")
		int updated,
		@Schema(description = "검색 결과나 thumbnail이 없어 건너뛴 수", example = "4")
		int skipped,
		@Schema(description = "외부 API 호출 또는 저장 실패 수", example = "1")
		int failed,
		List<ItemResult> items
) {

	public BookImageEnrichmentResponse(int processed, int updated, int skipped, int failed) {
		this(processed, updated, skipped, failed, List.of());
	}

	public record ItemResult(
			String bookId,
			String title,
			String author,
			String status,
			String reason,
			List<String> queries,
			String imageUrl
	) {
	}
}
