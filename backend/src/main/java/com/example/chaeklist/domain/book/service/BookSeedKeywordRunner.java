package com.example.chaeklist.domain.book.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookSeedKeywordRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BookSeedKeywordRunner.class);
	private static final List<SeedKeywordMapping> SEED_KEYWORD_MAPPINGS = List.of(
			new SeedKeywordMapping("dummy-sapiens", "역사"),
			new SeedKeywordMapping("dummy-sapiens", "문명"),
			new SeedKeywordMapping("dummy-money-psychology", "투자"),
			new SeedKeywordMapping("dummy-money-psychology", "경제"),
			new SeedKeywordMapping("dummy-atomic-habits", "습관"),
			new SeedKeywordMapping("dummy-atomic-habits", "자기관리"),
			new SeedKeywordMapping("dummy-human-acts", "소설"),
			new SeedKeywordMapping("dummy-human-acts", "역사"),
			new SeedKeywordMapping("dummy-why-fish", "과학"),
			new SeedKeywordMapping("dummy-why-fish", "에세이"),
			new SeedKeywordMapping("slow-reading", "독서"),
			new SeedKeywordMapping("slow-reading", "집중"),
			new SeedKeywordMapping("quiet-investing", "투자"),
			new SeedKeywordMapping("quiet-investing", "경제"),
			new SeedKeywordMapping("attention-design", "집중"),
			new SeedKeywordMapping("attention-design", "도파민"),
			new SeedKeywordMapping("small-city", "소설"),
			new SeedKeywordMapping("small-city", "관계"),
			new SeedKeywordMapping("daily-sentence", "문장"),
			new SeedKeywordMapping("daily-sentence", "에세이")
	);

	private final JdbcTemplate jdbcTemplate;

	public BookSeedKeywordRunner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		int inserted = applySeedKeywordMappings();
		if (inserted > 0) {
			log.info("Seed book keyword mappings applied. inserted={}", inserted);
		}
	}

	private int applySeedKeywordMappings() {
		return SEED_KEYWORD_MAPPINGS.stream()
				.mapToInt(this::insertSeedKeywordMapping)
				.sum();
	}

	private int insertSeedKeywordMapping(SeedKeywordMapping mapping) {
		jdbcTemplate.update("""
				INSERT INTO keywords (name, keyword_type, created_at)
				VALUES (?, 'TREND', CURRENT_TIMESTAMP)
				ON DUPLICATE KEY UPDATE name = VALUES(name)
				""", mapping.keywordName());
		return jdbcTemplate.update("""
				INSERT IGNORE INTO book_keywords (book_id, keyword_id)
				SELECT b.id, k.id
				FROM books b
				JOIN keywords k ON k.name = ?
					AND k.keyword_type = 'TREND'
				WHERE b.source_book_id = ?
					AND b.source_provider IN ('LOCAL', 'DUMMY')
					AND b.is_general_eligible = TRUE
				""", mapping.keywordName(), mapping.sourceBookId());
	}

	private record SeedKeywordMapping(String sourceBookId, String keywordName) {
	}
}
