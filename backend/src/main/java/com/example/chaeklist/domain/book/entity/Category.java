package com.example.chaeklist.domain.book.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Column(nullable = false, unique = true, length = 50)
	private String slug;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Category() {
	}

	public String name() {
		return name;
	}

	public int displayOrder() {
		return displayOrder;
	}

	public boolean active() {
		return active;
	}
}
