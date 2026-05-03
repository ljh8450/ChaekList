package com.example.chaeklist.domain.book.service;

import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
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
					AND (cover_image_url IS NULL OR cover_image_url = '' OR cover_image_url LIKE '%callback%')
				""", coverImageUrl, sourceBookId);
	}
}
