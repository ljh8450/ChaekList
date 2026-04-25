package com.example.chaeklist.domain.book.repository;

import java.util.List;

import com.example.chaeklist.domain.book.entity.BookRankingSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRankingSnapshotRepository extends JpaRepository<BookRankingSnapshot, Long> {

	@Query("""
			SELECT snapshot
			FROM BookRankingSnapshot snapshot
			JOIN FETCH snapshot.book book
			WHERE snapshot.category IS NULL
				AND snapshot.rankingPeriod = :period
				AND snapshot.rankDate = (
					SELECT MAX(latest.rankDate)
					FROM BookRankingSnapshot latest
					WHERE latest.category IS NULL
						AND latest.rankingPeriod = :period
				)
				AND book.generalEligible = true
			ORDER BY snapshot.rankPosition ASC
			""")
	List<BookRankingSnapshot> findLatestOverallRankings(@Param("period") String period, Pageable pageable);

	@Query("""
			SELECT snapshot
			FROM BookRankingSnapshot snapshot
			JOIN FETCH snapshot.book book
			JOIN snapshot.category category
			WHERE category.name = :category
				AND snapshot.rankingPeriod = :period
				AND snapshot.rankDate = (
					SELECT MAX(latest.rankDate)
					FROM BookRankingSnapshot latest
					JOIN latest.category latestCategory
					WHERE latestCategory.name = :category
						AND latest.rankingPeriod = :period
				)
				AND book.generalEligible = true
			ORDER BY snapshot.rankPosition ASC
			""")
	List<BookRankingSnapshot> findLatestCategoryRankings(
			@Param("category") String category,
			@Param("period") String period,
			Pageable pageable
	);

	@Query("""
			SELECT snapshot
			FROM BookRankingSnapshot snapshot
			JOIN FETCH snapshot.book book
			WHERE snapshot.category IS NULL
				AND snapshot.rankingPeriod = :period
				AND snapshot.rankDate = (
					SELECT MAX(latest.rankDate)
					FROM BookRankingSnapshot latest
					WHERE latest.category IS NULL
						AND latest.rankingPeriod = :period
				)
				AND book.generalEligible = true
			ORDER BY snapshot.recentGrowthRate DESC, snapshot.rankPosition ASC
			""")
	List<BookRankingSnapshot> findLatestTrending(@Param("period") String period, Pageable pageable);
}
