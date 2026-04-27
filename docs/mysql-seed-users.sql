-- ChaekList initial user dummy data
-- Run after docs/mysql-ddl.sql

USE chaeklist;

SET NAMES utf8mb4;

INSERT INTO users (email, nickname, password_hash, status)
VALUES
  ('minjun.kim@example.com', '민준', '$2a$10$dummyPasswordHashForLocalSeed000001', 'ACTIVE'),
  ('seoyeon.lee@example.com', '서연', '$2a$10$dummyPasswordHashForLocalSeed000002', 'ACTIVE'),
  ('jiho.park@example.com', '지호', '$2a$10$dummyPasswordHashForLocalSeed000003', 'ACTIVE'),
  ('hayeon.choi@example.com', '하연', '$2a$10$dummyPasswordHashForLocalSeed000004', 'ACTIVE'),
  ('doyun.jung@example.com', '도윤', '$2a$10$dummyPasswordHashForLocalSeed000005', 'ACTIVE'),
  ('yuna.kang@example.com', '유나', '$2a$10$dummyPasswordHashForLocalSeed000006', 'ACTIVE'),
  ('taeho.yoon@example.com', '태호', '$2a$10$dummyPasswordHashForLocalSeed000007', 'ACTIVE'),
  ('sumin.han@example.com', '수민', '$2a$10$dummyPasswordHashForLocalSeed000008', 'ACTIVE'),
  ('jaewon.lim@example.com', '재원', '$2a$10$dummyPasswordHashForLocalSeed000009', 'ACTIVE'),
  ('eunwoo.shin@example.com', '은우', '$2a$10$dummyPasswordHashForLocalSeed000010', 'ACTIVE');

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('humanities', 'self-development')
WHERE u.email = 'minjun.kim@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('fiction', 'essay')
WHERE u.email = 'seoyeon.lee@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('economy', 'self-development')
WHERE u.email = 'jiho.park@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('humanities', 'essay')
WHERE u.email = 'hayeon.choi@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('economy', 'humanities')
WHERE u.email = 'doyun.jung@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('fiction', 'humanities')
WHERE u.email = 'yuna.kang@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('self-development', 'economy')
WHERE u.email = 'taeho.yoon@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('essay', 'self-development')
WHERE u.email = 'sumin.han@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('fiction', 'economy')
WHERE u.email = 'jaewon.lim@example.com';

INSERT INTO user_interest_categories (user_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN categories c ON c.slug IN ('humanities', 'fiction')
WHERE u.email = 'eunwoo.shin@example.com';
