package com.example.chaeklist;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.chaeklist.domain.book.dto.BookImageEnrichmentResponse;
import com.example.chaeklist.domain.book.service.BookImageEnrichmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@JdbcTest
class BookImageEnrichmentServiceTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void replacesLocalSeedAssetCoverWithKakaoThumbnail() {
		createBooksTable();
		insertBookWithLocalSeedCover();
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		BookImageEnrichmentService service = new BookImageEnrichmentService(
				"test-kakao-key",
				"https://kakao.test/search/book",
				jdbcTemplate,
				restClientBuilder
		);
		server.expect(anything())
				.andRespond(withSuccess("""
						{
						  "documents": [
						    {
						      "title": "Slow Reading",
						      "authors": ["Moon"],
						      "thumbnail": "https://img.example.com/slow-reading.jpg"
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		BookImageEnrichmentResponse response = service.enrichMissingCoverImages(10);

		String coverImageUrl = jdbcTemplate.queryForObject("""
				SELECT cover_image_url
				FROM books
				WHERE id = ?
				""", String.class, 1L);
		org.assertj.core.api.Assertions.assertThat(response.processed()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(response.updated()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(coverImageUrl).isEqualTo("https://img.example.com/slow-reading.jpg");
		server.verify();
	}

	@Test
	void retriesWithTitleOnlyWhenTitleAuthorQueryHasNoThumbnail() {
		createBooksTable();
		insertBookWithLocalSeedCover();
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		BookImageEnrichmentService service = new BookImageEnrichmentService(
				"test-kakao-key",
				"https://kakao.test/search/book",
				jdbcTemplate,
				restClientBuilder
		);
		server.expect(anything())
				.andRespond(withSuccess("""
						{
						  "documents": []
						}
						""", MediaType.APPLICATION_JSON));
		server.expect(anything())
				.andRespond(withSuccess("""
						{
						  "documents": [
						    {
						      "title": "Slow Reading",
						      "authors": ["Moon"],
						      "thumbnail": "https://img.example.com/title-only.jpg"
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		BookImageEnrichmentResponse response = service.enrichMissingCoverImages(10);

		String coverImageUrl = jdbcTemplate.queryForObject("""
				SELECT cover_image_url
				FROM books
				WHERE id = ?
				""", String.class, 1L);
		org.assertj.core.api.Assertions.assertThat(response.updated()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(response.items().getFirst().queries())
				.containsExactly("Slow Reading Moon", "Slow Reading");
		org.assertj.core.api.Assertions.assertThat(coverImageUrl).isEqualTo("https://img.example.com/title-only.jpg");
		server.verify();
	}

	private void createBooksTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS books (
					id BIGINT PRIMARY KEY,
					title VARCHAR(255) NOT NULL,
					author VARCHAR(255) NOT NULL,
					isbn13 VARCHAR(13),
					cover_image_url VARCHAR(1000),
					source_provider VARCHAR(50),
					source_book_id VARCHAR(100),
					is_general_eligible BOOLEAN NOT NULL,
					updated_at TIMESTAMP NOT NULL
				)
				""");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS isbn13 VARCHAR(13)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(1000)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS source_provider VARCHAR(50)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS source_book_id VARCHAR(100)");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS is_general_eligible BOOLEAN");
		jdbcTemplate.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
	}

	private void insertBookWithLocalSeedCover() {
		jdbcTemplate.update("DELETE FROM books WHERE id = ?", 1L);
		jdbcTemplate.update("""
				INSERT INTO books (
					id, title, author, cover_image_url, source_provider, source_book_id,
					is_general_eligible, updated_at
				)
				VALUES (
					1, 'Slow Reading', 'Moon', '/book-covers/slow-reading.svg', 'LOCAL',
					'slow-reading', TRUE, CURRENT_TIMESTAMP
				)
				""");
	}
}
