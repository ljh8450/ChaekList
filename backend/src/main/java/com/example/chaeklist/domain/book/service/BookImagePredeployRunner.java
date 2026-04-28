package com.example.chaeklist.domain.book.service;

import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BookImagePredeployRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BookImagePredeployRunner.class);
	private static final int PREDEPLOY_LIMIT = 50;

	private final BookImageEnrichmentService bookImageEnrichmentService;

	public BookImagePredeployRunner(BookImageEnrichmentService bookImageEnrichmentService) {
		this.bookImageEnrichmentService = bookImageEnrichmentService;
	}

	@Override
	public void run(ApplicationArguments args) {
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
}
