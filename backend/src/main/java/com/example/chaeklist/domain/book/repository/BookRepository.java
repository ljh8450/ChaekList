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
}
