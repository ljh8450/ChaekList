package com.example.chaeklist.domain.mypage.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public enum ReadingPurpose {
	KNOWLEDGE(
			"지식 확장",
			"인문, 역사, 사회처럼 시야를 넓히는 책을 찾습니다.",
			Set.of("인문", "경제", "역사", "사회"),
			Set.of("AI", "역사", "사회")
	),
	SELF_IMPROVEMENT(
			"자기계발",
			"습관, 집중, 루틴처럼 실천에 도움이 되는 책을 찾습니다.",
			Set.of("자기계발"),
			Set.of("습관", "집중", "루틴")
	),
	ECONOMY_INVESTING(
			"경제/투자 이해",
			"경제 흐름과 투자 감각을 이해할 수 있는 책을 찾습니다.",
			Set.of("경제"),
			Set.of("투자", "돈", "시장")
	),
	LIGHT_READING(
			"가벼운 독서",
			"소설, 에세이처럼 부담 없이 읽을 수 있는 책을 찾습니다.",
			Set.of("소설", "에세이"),
			Set.of("일상", "관계")
	),
	TREND_TRACKING(
			"트렌드 파악",
			"AI, 기술, 트렌드처럼 지금 많이 이야기되는 주제를 찾습니다.",
			Set.of("경제"),
			Set.of("AI", "도파민", "트렌드", "기술", "투자")
	);

	private final String label;
	private final String description;
	private final Set<String> categoryNames;
	private final Set<String> keywords;

	ReadingPurpose(String label, String description, Set<String> categoryNames, Set<String> keywords) {
		this.label = label;
		this.description = description;
		this.categoryNames = categoryNames;
		this.keywords = keywords;
	}

	public String code() {
		return name();
	}

	public String label() {
		return label;
	}

	public String description() {
		return description;
	}

	public Set<String> categoryNames() {
		return categoryNames;
	}

	public Set<String> keywords() {
		return keywords;
	}

	public static List<ReadingPurpose> options() {
		return Arrays.asList(values());
	}

	public static Optional<ReadingPurpose> fromCode(String code) {
		if (code == null || code.isBlank()) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(purpose -> purpose.name().equals(code.trim().toUpperCase()))
				.findFirst();
	}
}
