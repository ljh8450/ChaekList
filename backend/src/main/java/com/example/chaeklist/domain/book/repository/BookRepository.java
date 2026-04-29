package com.example.chaeklist.domain.book.repository;

import java.util.List;
import java.util.Optional;

import com.example.chaeklist.domain.book.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

	List<Book> findByGeneralEligibleTrue(Pageable pageable);

	Optional<Book> findByIdAndGeneralEligibleTrue(Long id);

	List<Book> findByCategoriesNameAndGeneralEligibleTrue(String category, Pageable pageable);

	List<Book> findByCategoriesNameAndGeneralEligibleTrueAndIdNot(String category, Long id, Pageable pageable);

	@Query("""
			SELECT b
			FROM Book b
			WHERE b.generalEligible = TRUE
				AND (
					LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
					OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))
				)
			""")
	List<Book> searchGeneralEligibleByTitleOrAuthor(@Param("query") String query, Pageable pageable);

	@Query("""
			SELECT b
			FROM Book b
			JOIN b.keywords keyword
			LEFT JOIN BookRankingSnapshot snapshot
				ON snapshot.book = b
				AND snapshot.category IS NULL
				AND snapshot.rankingPeriod = :period
				AND snapshot.rankDate = (
					SELECT MAX(latest.rankDate)
					FROM BookRankingSnapshot latest
					WHERE latest.category IS NULL
						AND latest.rankingPeriod = :period
				)
			WHERE b.generalEligible = TRUE
				AND keyword.name = :keyword
				AND keyword.keywordType = 'TREND'
			ORDER BY COALESCE(snapshot.recentGrowthRate, 0) DESC,
				COALESCE(snapshot.rankingScore, 0) DESC,
				b.id DESC
			""")
	List<Book> findTrendingBooksByKeyword(
			@Param("keyword") String keyword,
			@Param("period") String period,
			Pageable pageable
	);
}
