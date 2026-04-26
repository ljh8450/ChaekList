# 마이페이지 구현 리뷰

작성일: 2026-04-26

## 구현 범위 요약

- `GET /api/me/mypage` API가 추가되었습니다.
- 마이페이지 API는 인증된 사용자 기준으로 다음 데이터를 조회합니다.
  - 관심 분야: `user_interest_categories`, `categories`
  - 읽은 책: `user_book_interactions`, `books`, `book_categories`
  - 저장한 책: `user_book_interactions`, `books`, `book_categories`
  - 추천 히스토리: `recommendations`, `books`
- frontend 마이페이지는 기존 mock 배열 대신 `/api/me/mypage` 응답을 사용하도록 변경되었습니다.
- 로그인 상태는 `localStorage`에 저장한 access token을 `/api/auth/me`로 검증해 새로고침 후 복원합니다.

## 발견된 이슈

### 1. 저장 수 집계 기준 불일치

`MyPageService#getSavedBooks()`에서 전체 저장 수 집계를 위한 `all_save_interactions` 조인이 있지만, 실제 `save_count`는 현재 사용자의 `save_interactions.id`를 기준으로 계산됩니다.

현재 프론트 카드의 `saves`는 책의 전체 저장 수처럼 보이므로, backend 응답도 `COUNT(DISTINCT all_save_interactions.id)` 기준으로 맞추는 것이 좋습니다.

### 2. 책의 다중 카테고리 중복 가능성

`book_categories`를 직접 조인하고 있어 한 책이 여러 카테고리에 속하면 읽은 책/저장한 책 목록에서 중복 row가 생길 수 있습니다.

대표 카테고리는 `is_primary = TRUE`를 우선하고, 없으면 `display_order`가 가장 낮은 카테고리 1개를 선택하는 방식이 필요합니다.

### 3. access token 만료 시 로그인 유지 한계

현재 새로고침 복원은 저장된 access token을 `/api/auth/me`로 검증하는 방식입니다.

access token이 만료되면 refresh token으로 재발급하지 않고 로그아웃 처리됩니다. MVP로는 동작하지만, 실제 로그인 유지 UX는 아직 약합니다.

### 4. 마이페이지 빈 상태 UX 부족

API 호출 실패 시 오류 메시지는 표시되지만, 관심 분야/읽은 책/저장한 책/추천 히스토리가 각각 0건인 경우 섹션별 빈 상태 문구가 없습니다.

데이터가 없는 정상 상태와 API 실패 상태를 구분해서 보여주는 보완이 필요합니다.

### 5. 테스트 케이스 부족

현재 마이페이지 테스트는 happy path와 토큰 누락을 검증합니다.

다음 케이스가 추가로 필요합니다.

- 관심 분야가 0건인 사용자
- 읽은 책이 0건인 사용자
- 추천 히스토리가 0건인 사용자
- 한 책이 여러 카테고리에 속한 경우
- `SAVE` 이후 `UNSAVE`된 책
- 다른 사용자의 `SAVE`까지 포함한 전체 저장 수 집계

## 검증 결과

- backend: `.\gradlew.bat test` 성공
- frontend: `npm run build` 성공

## 보완 계획

### 1순위

- `getSavedBooks()`의 `save_count`를 전체 SAVE 기준으로 수정합니다.
- 다중 카테고리 책이 중복되지 않도록 대표 카테고리 선택 SQL을 분리합니다.
- 관련 테스트를 추가합니다.

### 2순위

- frontend 마이페이지에 섹션별 빈 상태 문구를 추가합니다.
- API 실패 상태와 정상 빈 데이터 상태를 분리합니다.

### 3순위

- `/api/auth/refresh` API를 추가합니다.
- frontend에서 `/api/auth/me` 또는 `/api/me/mypage`가 401을 반환하면 refresh token으로 access token을 재발급하고 요청을 재시도합니다.

## 체크리스트

- [ ] 저장 수 집계 기준 수정
- [ ] 대표 카테고리 1개 선택 로직 추가
- [ ] 마이페이지 빈 상태 UI 추가
- [ ] refresh token 재발급 API 추가
- [ ] frontend 인증 재시도 흐름 추가
- [ ] 마이페이지 edge case 테스트 확장
