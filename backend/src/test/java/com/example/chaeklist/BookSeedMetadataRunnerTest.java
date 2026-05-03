package com.example.chaeklist;

import com.example.chaeklist.domain.book.service.BookSeedMetadataRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
class BookSeedMetadataRunnerTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void updatesLocalSeedBookToRealBookMetadata() {
		createBooksTable();
		insertLocalSeedBook();
		BookSeedMetadataRunner runner = new BookSeedMetadataRunner(jdbcTemplate);

		runner.run(null);

		SeedBook seedBook = jdbcTemplate.queryForObject("""
				SELECT title, author, publisher
				FROM books
				WHERE source_book_id = ?
				""",
				(resultSet, rowNumber) -> new SeedBook(
						resultSet.getString("title"),
						resultSet.getString("author"),
						resultSet.getString("publisher")
				),
				"slow-reading"
		);
		org.assertj.core.api.Assertions.assertThat(seedBook)
				.isEqualTo(new SeedBook("책은 도끼다", "박웅현", "북하우스"));
	}

	private void createBooksTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS books (
					id BIGINT PRIMARY KEY,
					title VARCHAR(255) NOT NULL,
					author VARCHAR(255) NOT NULL,
					publisher VARCHAR(100),
					source_provider VARCHAR(50),
					source_book_id VARCHAR(100),
					is_general_eligible BOOLEAN NOT NULL,
					filter_status VARCHAR(20) NOT NULL,
					created_at TIMESTAMP NOT NULL,
					updated_at TIMESTAMP NOT NULL
				)
				""");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS publisher VARCHAR(100)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS source_provider VARCHAR(50)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS source_book_id VARCHAR(100)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS is_general_eligible BOOLEAN");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS filter_status VARCHAR(20)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
	}

	private void insertLocalSeedBook() {
		jdbcTemplate.update("DELETE FROM books WHERE id = ?", 1L);
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, publisher, source_provider, source_book_id,
					is_general_eligible, filter_status, created_at, updated_at
				)
				VALUES (
					1, '느리게 읽는 힘', '문서윤', NULL, 'LOCAL', 'slow-reading',
					TRUE, 'INCLUDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
				)
				""");
	}

	private record SeedBook(String title, String author, String publisher) {
	}
}
