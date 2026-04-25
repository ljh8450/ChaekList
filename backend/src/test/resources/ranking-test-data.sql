INSERT INTO categories (id, name, slug, display_order, is_active, created_at, updated_at)
VALUES
  (101, '인문', 'humanities-test', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, '경제', 'economy-test', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (
  id,
  title,
  author,
  is_general_eligible,
  filter_status,
  filter_reason,
  created_at,
  updated_at
)
VALUES
  (101, '느리게 읽는 힘', '문서윤', TRUE, 'INCLUDED', '교양 필터 통과', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, '조용한 투자 습관', '한도윤', TRUE, 'INCLUDED', '교양 필터 통과', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (103, '기출 경제학', '시험연구소', FALSE, 'EXCLUDED', '수험서 제외', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO book_categories (book_id, category_id)
VALUES
  (101, 101),
  (102, 102),
  (103, 102);

INSERT INTO book_ranking_snapshots (
  id,
  book_id,
  category_id,
  ranking_period,
  rank_date,
  rank_position,
  ranking_score,
  view_count,
  click_count,
  save_count,
  review_count,
  recent_growth_rate,
  created_at
)
VALUES
  (101, 101, NULL, 'WEEKLY', DATE '2026-04-20', 2, 71.0000, 1200, 300, 80, 10, 8.0000, CURRENT_TIMESTAMP),
  (102, 102, NULL, 'WEEKLY', DATE '2026-04-20', 1, 95.0000, 2400, 500, 120, 20, 15.0000, CURRENT_TIMESTAMP),
  (103, 103, NULL, 'WEEKLY', DATE '2026-04-20', 3, 99.0000, 9000, 800, 300, 40, 50.0000, CURRENT_TIMESTAMP),
  (104, 102, 102, 'WEEKLY', DATE '2026-04-20', 1, 92.0000, 1800, 320, 90, 15, 18.0000, CURRENT_TIMESTAMP),
  (105, 101, NULL, 'WEEKLY', DATE '2026-04-13', 1, 99.0000, 5000, 1000, 300, 50, 30.0000, CURRENT_TIMESTAMP);
