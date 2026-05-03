-- ChaekList initial MySQL DDL
-- Target: MySQL 8.x

CREATE DATABASE IF NOT EXISTS chaeklist
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE chaeklist;

SET NAMES utf8mb4;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_nickname (nickname),
  CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
  CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  slug VARCHAR(50) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_name (name),
  UNIQUE KEY uk_categories_slug (slug)
) ENGINE=InnoDB;

CREATE TABLE books (
  id BIGINT NOT NULL AUTO_INCREMENT,
  isbn13 VARCHAR(13) NULL,
  title VARCHAR(255) NOT NULL,
  subtitle VARCHAR(255) NULL,
  author VARCHAR(255) NOT NULL,
  publisher VARCHAR(100) NULL,
  published_date DATE NULL,
  description TEXT NULL,
  cover_image_url VARCHAR(1000) NULL,
  source_provider VARCHAR(50) NULL,
  source_book_id VARCHAR(100) NULL,
  is_general_eligible BOOLEAN NOT NULL DEFAULT TRUE,
  filter_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  filter_reason VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_books_isbn13 (isbn13),
  UNIQUE KEY uk_books_source (source_provider, source_book_id),
  KEY idx_books_title (title),
  KEY idx_books_filter_status (filter_status),
  KEY idx_books_general_eligible (is_general_eligible),
  CONSTRAINT chk_books_filter_status CHECK (filter_status IN ('PENDING', 'INCLUDED', 'EXCLUDED'))
) ENGINE=InnoDB;

CREATE TABLE book_categories (
  book_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (book_id, category_id),
  KEY idx_book_categories_category_id (category_id),
  CONSTRAINT fk_book_categories_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_book_categories_category
    FOREIGN KEY (category_id) REFERENCES categories (id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE keywords (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  keyword_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_keywords_name_type (name, keyword_type),
  KEY idx_keywords_type (keyword_type),
  CONSTRAINT chk_keywords_type CHECK (keyword_type IN ('GENERAL', 'TREND', 'EXCLUDE'))
) ENGINE=InnoDB;

CREATE TABLE book_keywords (
  book_id BIGINT NOT NULL,
  keyword_id BIGINT NOT NULL,
  weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (book_id, keyword_id),
  KEY idx_book_keywords_keyword_id (keyword_id),
  CONSTRAINT fk_book_keywords_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_book_keywords_keyword
    FOREIGN KEY (keyword_id) REFERENCES keywords (id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_interest_categories (
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, category_id),
  KEY idx_user_interest_categories_category_id (category_id),
  CONSTRAINT fk_user_interest_categories_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_user_interest_categories_category
    FOREIGN KEY (category_id) REFERENCES categories (id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE user_book_interactions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  interaction_type VARCHAR(30) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_user_book_interactions_user_created (user_id, created_at),
  KEY idx_user_book_interactions_book_type (book_id, interaction_type),
  CONSTRAINT fk_user_book_interactions_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_user_book_interactions_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_user_book_interactions_type CHECK (
    interaction_type IN ('VIEW', 'CLICK', 'SAVE', 'READ', 'UNSAVE')
  )
) ENGINE=InnoDB;

CREATE TABLE reviews (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  rating TINYINT NULL,
  content TEXT NULL,
  sentiment_score DECIMAL(5,4) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_reviews_user_book (user_id, book_id),
  KEY idx_reviews_book_created (book_id, created_at),
  CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_reviews_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_reviews_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE book_ranking_snapshots (
  id BIGINT NOT NULL AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  category_id BIGINT NULL,
  ranking_period VARCHAR(10) NOT NULL,
  rank_date DATE NOT NULL,
  rank_position INT NOT NULL,
  ranking_score DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
  view_count INT NOT NULL DEFAULT 0,
  click_count INT NOT NULL DEFAULT 0,
  save_count INT NOT NULL DEFAULT 0,
  review_count INT NOT NULL DEFAULT 0,
  recent_growth_rate DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_book_ranking_snapshot (book_id, category_id, ranking_period, rank_date),
  KEY idx_book_ranking_lookup (category_id, ranking_period, rank_date, rank_position),
  CONSTRAINT fk_book_ranking_snapshots_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_book_ranking_snapshots_category
    FOREIGN KEY (category_id) REFERENCES categories (id)
    ON DELETE SET NULL,
  CONSTRAINT chk_book_ranking_period CHECK (ranking_period IN ('DAILY', 'WEEKLY', 'MONTHLY'))
) ENGINE=InnoDB;

CREATE TABLE trend_keywords (
  id BIGINT NOT NULL AUTO_INCREMENT,
  keyword_id BIGINT NOT NULL,
  trend_date DATE NOT NULL,
  rank_position INT NOT NULL,
  mention_count INT NOT NULL DEFAULT 0,
  growth_rate DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_trend_keywords_date_rank (trend_date, rank_position),
  UNIQUE KEY uk_trend_keywords_keyword_date (keyword_id, trend_date),
  CONSTRAINT fk_trend_keywords_keyword
    FOREIGN KEY (keyword_id) REFERENCES keywords (id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE recommendations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  recommendation_type VARCHAR(30) NOT NULL,
  reason VARCHAR(255) NULL,
  score DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_recommendations_user_book_type (user_id, book_id, recommendation_type),
  KEY idx_recommendations_user_score (user_id, score),
  CONSTRAINT fk_recommendations_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_recommendations_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_recommendation_type CHECK (
    recommendation_type IN ('CONTENT_BASED', 'COLLABORATIVE', 'TRENDING', 'EDITORIAL')
  )
) ENGINE=InnoDB;

CREATE TABLE feed_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  feed_type VARCHAR(30) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  book_id BIGINT NULL,
  keyword_id BIGINT NULL,
  display_order INT NOT NULL DEFAULT 0,
  starts_at DATETIME(6) NULL,
  ends_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_feed_items_type_order (feed_type, display_order),
  CONSTRAINT fk_feed_items_book
    FOREIGN KEY (book_id) REFERENCES books (id)
    ON DELETE SET NULL,
  CONSTRAINT fk_feed_items_keyword
    FOREIGN KEY (keyword_id) REFERENCES keywords (id)
    ON DELETE SET NULL,
  CONSTRAINT chk_feed_items_type CHECK (
    feed_type IN ('RISING', 'TODAY_RECOMMENDATION', 'THEME_CURATION')
  )
) ENGINE=InnoDB;

CREATE TABLE user_notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  notification_type VARCHAR(30) NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  target_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  message VARCHAR(255) NOT NULL,
  read_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_user_notifications_user_created (user_id, created_at),
  KEY idx_user_notifications_user_read_created (user_id, read_at, created_at),
  CONSTRAINT fk_user_notifications_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_user_notifications_type CHECK (
    notification_type IN ('LIKE', 'REPORT_STATUS', 'SERVICE')
  ),
  CONSTRAINT chk_user_notifications_target_type CHECK (
    target_type IN ('POST', 'REPORT', 'SERVICE')
  )
) ENGINE=InnoDB;

INSERT INTO categories (name, slug, display_order)
VALUES
  ('인문', 'humanities', 1),
  ('자기계발', 'self-development', 2),
  ('경제', 'economy', 3),
  ('소설', 'fiction', 4),
  ('에세이', 'essay', 5);

INSERT INTO keywords (name, keyword_type)
VALUES
  ('기출', 'EXCLUDE'),
  ('문제집', 'EXCLUDE'),
  ('수능', 'EXCLUDE'),
  ('기사', 'EXCLUDE'),
  ('전공', 'EXCLUDE'),
  ('도파민', 'TREND'),
  ('투자', 'TREND'),
  ('AI', 'TREND');
