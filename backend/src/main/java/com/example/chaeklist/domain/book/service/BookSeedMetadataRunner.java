package com.example.chaeklist.domain.book.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class BookSeedMetadataRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BookSeedMetadataRunner.class);
	private static final List<SeedBookMetadata> SEED_BOOKS = List.of(
			new SeedBookMetadata("dummy-sapiens", "사피엔스", "유발 하라리", "김영사"),
			new SeedBookMetadata("dummy-money-psychology", "돈의 심리학", "모건 하우절", "인플루엔셜"),
			new SeedBookMetadata("dummy-atomic-habits", "아주 작은 습관의 힘", "제임스 클리어", "비즈니스북스"),
			new SeedBookMetadata("dummy-human-acts", "소년이 온다", "한강", "창비"),
			new SeedBookMetadata("dummy-why-fish", "물고기는 존재하지 않는다", "룰루 밀러", "곰출판"),
			new SeedBookMetadata("slow-reading", "책은 도끼다", "박웅현", "북하우스"),
			new SeedBookMetadata("quiet-investing", "돈의 심리학", "모건 하우절", "인플루엔셜"),
			new SeedBookMetadata("attention-design", "아주 작은 습관의 힘", "제임스 클리어", "비즈니스북스"),
			new SeedBookMetadata("small-city", "불편한 편의점", "김호연", "나무옆의자"),
			new SeedBookMetadata("daily-sentence", "하루 한 장 나의 어휘력을 위한 필사 노트", "유선경", "위즈덤하우스")
	);

	private final JdbcTemplate jdbcTemplate;

	public BookSeedMetadataRunner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		int updated = SEED_BOOKS.stream()
				.mapToInt(this::updateSeedBook)
				.sum();
		if (updated > 0) {
			log.info("Seed book metadata applied. updated={}", updated);
		}
	}

	private int updateSeedBook(SeedBookMetadata seedBook) {
		return jdbcTemplate.update("""
				UPDATE books
				SET title = ?,
					author = ?,
					publisher = ?,
					updated_at = CURRENT_TIMESTAMP
				WHERE source_book_id = ?
					AND source_provider IN ('LOCAL', 'DUMMY')
				""", seedBook.title(), seedBook.author(), seedBook.publisher(), seedBook.sourceBookId());
	}

	private record SeedBookMetadata(String sourceBookId, String title, String author, String publisher) {
	}
}
