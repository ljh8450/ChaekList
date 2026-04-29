package com.example.chaeklist.domain.book.service;

import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookImagePredeployRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BookImagePredeployRunner.class);
	private static final int PREDEPLOY_LIMIT = 50;

	private final BookImageEnrichmentService bookImageEnrichmentService;
	private final JdbcTemplate jdbcTemplate;

	public BookImagePredeployRunner(BookImageEnrichmentService bookImageEnrichmentService, JdbcTemplate jdbcTemplate) {
		this.bookImageEnrichmentService = bookImageEnrichmentService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		int seededCoverUrls = applySeedCoverUrls();
		if (seededCoverUrls > 0) {
			log.info("Seed book cover URLs applied. updated={}", seededCoverUrls);
		}
		int seededKeywordMappings = applySeedKeywordMappings();
		if (seededKeywordMappings > 0) {
			log.info("Seed book keyword mappings applied. inserted={}", seededKeywordMappings);
		}

		try {
			BookImageEnrichmentResponse response = bookImageEnrichmentService.enrichMissingCoverImages(PREDEPLOY_LIMIT);
			log.info(
					"Book image predeploy completed. processed={}, updated={}, skipped={}, failed={}",
					response.processed(),
					response.updated(),
					response.skipped(),
					response.failed()
			);
		} catch (BookImageEnrichmentService.BookImageEnrichmentException exception) {
			log.warn("Book image predeploy skipped. {}", exception.getMessage());
		} catch (RuntimeException exception) {
			log.warn("Book image predeploy failed.", exception);
		}
	}

	private int applySeedCoverUrls() {
		int updated = 0;
		updated += updateSeedCoverUrl("dummy-atomic-habits", "/book-covers/atomic-habits.svg");
		updated += updateSeedCoverUrl("slow-reading", "/book-covers/slow-reading.svg");
		updated += updateSeedCoverUrl("quiet-investing", "/book-covers/quiet-investing.svg");
		updated += updateSeedCoverUrl("attention-design", "/book-covers/attention-design.svg");
		updated += updateSeedCoverUrl("small-city", "/book-covers/small-city.svg");
		updated += updateSeedCoverUrl("daily-sentence", "/book-covers/daily-sentence.svg");
		updated += updateSeedCoverUrl("excluded-economics-test", "/book-covers/excluded-economics-test.svg");
		return updated;
	}

	private int updateSeedCoverUrl(String sourceBookId, String coverImageUrl) {
		return jdbcTemplate.update("""
				UPDATE books
				SET cover_image_url = ?,
					updated_at = CURRENT_TIMESTAMP
				WHERE source_book_id = ?
					AND source_provider IN ('LOCAL', 'DUMMY')
					AND (cover_image_url IS NULL OR cover_image_url = '')
				""", coverImageUrl, sourceBookId);
	}

	private int applySeedKeywordMappings() {
		int inserted = 0;
		inserted += insertSeedKeywordMapping("dummy-sapiens", "역사");
		inserted += insertSeedKeywordMapping("dummy-sapiens", "문명");
		inserted += insertSeedKeywordMapping("dummy-money-psychology", "투자");
		inserted += insertSeedKeywordMapping("dummy-money-psychology", "경제");
		inserted += insertSeedKeywordMapping("dummy-atomic-habits", "습관");
		inserted += insertSeedKeywordMapping("dummy-atomic-habits", "자기관리");
		inserted += insertSeedKeywordMapping("dummy-human-acts", "소설");
		inserted += insertSeedKeywordMapping("dummy-human-acts", "역사");
		inserted += insertSeedKeywordMapping("dummy-why-fish", "과학");
		inserted += insertSeedKeywordMapping("dummy-why-fish", "에세이");
		inserted += insertSeedKeywordMapping("slow-reading", "독서");
		inserted += insertSeedKeywordMapping("slow-reading", "집중");
		inserted += insertSeedKeywordMapping("quiet-investing", "투자");
		inserted += insertSeedKeywordMapping("quiet-investing", "경제");
		inserted += insertSeedKeywordMapping("attention-design", "집중");
		inserted += insertSeedKeywordMapping("attention-design", "도파민");
		inserted += insertSeedKeywordMapping("small-city", "소설");
		inserted += insertSeedKeywordMapping("small-city", "관계");
		inserted += insertSeedKeywordMapping("daily-sentence", "문장");
		inserted += insertSeedKeywordMapping("daily-sentence", "에세이");
		return inserted;
	}

	private int insertSeedKeywordMapping(String sourceBookId, String keywordName) {
		jdbcTemplate.update("""
				INSERT INTO keywords (name, keyword_type, created_at)
				VALUES (?, 'TREND', CURRENT_TIMESTAMP)
				ON DUPLICATE KEY UPDATE name = VALUES(name)
				""", keywordName);
		return jdbcTemplate.update("""
				INSERT IGNORE INTO book_keywords (book_id, keyword_id)
				SELECT b.id, k.id
				FROM books b
				JOIN keywords k ON k.name = ?
					AND k.keyword_type = 'TREND'
				WHERE b.source_book_id = ?
					AND b.source_provider IN ('LOCAL', 'DUMMY')
					AND b.is_general_eligible = TRUE
				""", keywordName, sourceBookId);
	}
}
