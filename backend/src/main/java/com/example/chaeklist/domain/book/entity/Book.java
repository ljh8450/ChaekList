package com.example.chaeklist.domain.book.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 13)
	private String isbn13;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(length = 255)
	private String subtitle;

	@Column(nullable = false, length = 255)
	private String author;

	@Column(length = 100)
	private String publisher;

	@Column(name = "published_date")
	private LocalDate publishedDate;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "cover_image_url", length = 1000)
	private String coverImageUrl;

	@Column(name = "source_provider", length = 50)
	private String sourceProvider;

	@Column(name = "source_book_id", length = 100)
	private String sourceBookId;

	@Column(name = "is_general_eligible", nullable = false)
	private boolean generalEligible;

	@Column(name = "filter_status", nullable = false, length = 20)
	private String filterStatus;

	@Column(name = "filter_reason", length = 255)
	private String filterReason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "book_categories",
			joinColumns = @JoinColumn(name = "book_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id")
	)
	private Set<Category> categories = new LinkedHashSet<>();

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "book_keywords",
			joinColumns = @JoinColumn(name = "book_id"),
			inverseJoinColumns = @JoinColumn(name = "keyword_id")
	)
	private Set<Keyword> keywords = new LinkedHashSet<>();

	protected Book() {
	}

	public String id() {
		return id == null ? "" : id.toString();
	}

	public Long numericId() {
		return id;
	}

	public String title() {
		return title;
	}

	public String author() {
		return author;
	}

	public String coverImageUrl() {
		return coverImageUrl;
	}

	public String category() {
		return categories.stream()
				.min(Comparator.comparingInt(Category::displayOrder))
				.map(Category::name)
				.orElse("미분류");
	}

	public String tag() {
		if (filterReason != null && !filterReason.isBlank()) {
			return filterReason;
		}
		return generalEligible ? "교양 필터 통과" : filterStatus;
	}

	public boolean generalEligible() {
		return generalEligible;
	}

	public String filterStatus() {
		return filterStatus;
	}

	public String filterReason() {
		return filterReason;
	}

	public String summary() {
		return description == null ? "" : description;
	}

	public String recommendationReason() {
		String category = category();
		List<String> keywordNames = keywords();

		if (!keywordNames.isEmpty() && !"미분류".equals(category)) {
			return keywordNames.getFirst() + " 키워드와 관련된 " + category + " 분야 교양 도서입니다.";
		}

		if (!"미분류".equals(category)) {
			return "최근 " + category + " 분야에서 교양 필터를 통과해 추천됩니다.";
		}

		return "교양 필터를 통과한 도서로 추천됩니다.";
	}

	public String views() {
		return "0";
	}

	public int saves() {
		return 0;
	}

	public int growthRate() {
		return 0;
	}

	public List<String> keywords() {
		return keywords.stream()
				.map(Keyword::name)
				.sorted()
				.toList();
	}
}
