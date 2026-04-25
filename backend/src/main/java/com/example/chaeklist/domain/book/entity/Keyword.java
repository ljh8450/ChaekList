package com.example.chaeklist.domain.book.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "keywords")
public class Keyword {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "keyword_type", nullable = false, length = 30)
	private String keywordType;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected Keyword() {
	}

	public String name() {
		return name;
	}
}
