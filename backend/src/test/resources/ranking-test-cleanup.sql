DELETE FROM book_ranking_snapshots WHERE id IN (101, 102, 103, 104, 105);
DELETE FROM book_categories WHERE book_id IN (101, 102, 103);
DELETE FROM books WHERE id IN (101, 102, 103);
DELETE FROM categories WHERE id IN (101, 102);
