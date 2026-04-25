package com.example.chaeklist.domain.book.repository;

import java.util.List;

import com.example.chaeklist.domain.book.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findByActiveTrueOrderByDisplayOrderAsc();

	boolean existsByName(String name);
}
