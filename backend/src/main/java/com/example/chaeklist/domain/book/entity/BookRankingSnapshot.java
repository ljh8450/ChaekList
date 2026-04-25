package com.example.chaeklist.domain.book.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_ranking_snapshots")
public class BookRankingSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(name = "ranking_period", nullable = false, length = 10)
	private String rankingPeriod;

	@Column(name = "rank_date", nullable = false)
	private LocalDate rankDate;

	@Column(name = "rank_position", nullable = false)
	private int rankPosition;

	@Column(name = "ranking_score", nullable = false, precision = 12, scale = 4)
	private BigDecimal rankingScore;

	@Column(name = "view_count", nullable = false)
	private int viewCount;

	@Column(name = "click_count", nullable = false)
	private int clickCount;

	@Column(name = "save_count", nullable = false)
	private int saveCount;

	@Column(name = "review_count", nullable = false)
	private int reviewCount;

	@Column(name = "recent_growth_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal recentGrowthRate;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected BookRankingSnapshot() {
	}

	public Book book() {
		return book;
	}

	public String rankingPeriod() {
		return rankingPeriod;
	}

	public LocalDate rankDate() {
		return rankDate;
	}

	public int rankPosition() {
		return rankPosition;
	}

	public int viewCount() {
		return viewCount;
	}

	public int saveCount() {
		return saveCount;
	}

	public BigDecimal recentGrowthRate() {
		return recentGrowthRate;
	}
}
