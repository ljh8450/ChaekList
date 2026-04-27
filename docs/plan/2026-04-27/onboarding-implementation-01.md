# Onboarding Implementation Plan

## Goal

회원가입 이후 사용자가 관심 분야와 읽은 책을 입력하고, 이 값이 마이페이지와 추천 근거에 반영되는 1차 온보딩 흐름을 구현한다.

## Current Context

- 기존 계획 문서: `docs/review/review-2026-04-26-plan.md`
- 앱 구조:
  - `frontend/`: React UI
  - `backend/`: Spring Boot REST API
- 현재 마이페이지는 `GET /api/me/mypage`로 관심 분야, 읽은 책, 저장한 책, 추천 히스토리를 조회한다.
- 현재 DB에는 온보딩 전용 완료 컬럼은 없지만, 다음 테이블은 이미 존재한다.
  - `user_interest_categories`
  - `user_book_interactions`
  - `recommendations`

## Phase 1 Scope

1차 구현 범위는 다음으로 제한한다.

- 회원가입 후 온보딩 화면 진입
- 최초 로그인 시 온보딩 완료 여부 확인
- 관심 분야 선택
- 읽은 책 선택
- 온보딩 저장 후 마이페이지 이동
- 저장된 관심 분야와 읽은 책을 기존 마이페이지에 반영

읽서 목적 저장, 추천 알고리즘 고도화, 책 상세 행동 버튼은 1차 범위에서 제외한다.

## Onboarding Display Rules

온보딩은 회원가입 이후 또는 최초 로그인 시에만 보여야 한다.

권장 1차 기준:

- 별도 DB schema 변경 없이 기존 데이터를 기준으로 완료 여부를 판단한다.
- `user_interest_categories`에 사용자 관심 분야가 1개 이상 있으면 온보딩 완료로 본다.
- 필요하면 `user_book_interactions`의 `READ` 기록도 보조 조건으로 확인한다.

프론트엔드 흐름:

1. 회원가입 성공 후 로그인 세션을 저장한다.
2. 온보딩 완료 여부를 조회한다.
3. 완료되지 않았으면 `/onboarding`으로 이동한다.
4. 완료된 사용자라면 `/mypage` 또는 기존 목적지로 이동한다.
5. 두 번째 로그인부터는 완료 여부가 true이므로 온보딩을 다시 보여주지 않는다.

명확한 장기 대안:

- `users.onboarding_completed` 같은 컬럼을 추가하면 가장 명확하다.
- 이 방식은 DB schema 변경이므로 별도 사용자 승인이 필요하다.

## Backend Plan

기존 `MyPageController`와 `MyPageService` 패턴을 우선 재사용한다.

추가 API 후보:

- `GET /api/me/onboarding-status`
  - 인증 필요
  - 현재 사용자의 온보딩 완료 여부 반환
  - 응답 예: `{ "completed": true }`
- `GET /api/me/onboarding-options`
  - 인증 필요
  - 활성 카테고리와 선택 가능한 책 목록 반환
- `PUT /api/me/onboarding`
  - 인증 필요
  - 관심 분야 ID 목록과 읽은 책 ID 목록 저장
  - 기존 관심 분야를 교체 저장한다.
  - 읽은 책은 `user_book_interactions`에 `READ` interaction으로 기록한다.

예상 변경 파일:

- `backend/src/main/java/com/example/chaeklist/domain/mypage/controller/MyPageController.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/*`
- `backend/src/test/java/com/example/chaeklist/MyPageControllerTest.java`

주의 사항:

- DB schema 변경은 하지 않는다.
- 신규 테이블을 만들지 않는다.
- 저장 API는 존재하지 않는 category/book ID를 거부하거나 무시하는 기준을 명확히 정한다.

## Frontend Plan

신규 온보딩 페이지를 추가한다.

예상 변경 파일:

- `frontend/src/App.jsx`
- `frontend/src/components/AuthForm.jsx`
- `frontend/src/pages/OnboardingPage.jsx`
- 필요 시 `frontend/src/styles/globals.css`

화면 흐름:

1. `/onboarding` 라우트를 추가한다.
2. 인증된 사용자만 접근 가능하게 한다.
3. 관심 분야 선택 섹션을 제공한다.
4. 읽은 책 선택 섹션을 제공한다.
5. 저장 성공 시 `/mypage`로 이동한다.
6. 이미 온보딩 완료 상태면 `/mypage`로 이동한다.

회원가입/로그인 연동:

- 회원가입 성공 후 온보딩 상태를 확인한다.
- 완료되지 않은 사용자만 `/onboarding`으로 이동한다.
- 로그인 사용자는 온보딩 완료 여부에 따라 기존 목적지 또는 `/onboarding`으로 이동한다.

## My Page Reflection

마이페이지는 기존 `GET /api/me/mypage` 응답을 계속 사용한다.

추가 개선:

- 관심 분야 또는 읽은 책이 비어 있으면 다음 행동 버튼을 보여준다.
- 예: "온보딩 입력하기" 또는 "취향 수정하기"
- 온보딩 저장 후 마이페이지에서 관심 분야와 읽은 책이 바로 확인되어야 한다.

## Phase 2 Candidates

다음 항목은 계획에 명시하되 1차 구현에서는 제외한다.

- 책 상세의 `저장하기` 버튼
- 책 상세의 `읽었어요` 버튼
- 책 상세의 `관심 없음` 버튼
- 사용자 행동 기반 추천 이유 고도화
- 추천 히스토리와 추천 점수 반영 개선
- 검색 기반 읽은 책 추가
- 독서 목적 저장 및 추천 반영

2차 범위는 1차 온보딩 저장 흐름이 안정화된 뒤 별도 계획으로 진행한다.

## Implementation Order

1. 백엔드 온보딩 상태 조회 API를 추가한다.
2. 백엔드 온보딩 옵션 조회 API를 추가한다.
3. 백엔드 온보딩 저장 API를 추가한다.
4. 백엔드 테스트를 추가하거나 기존 `MyPageControllerTest`를 확장한다.
5. 프론트엔드 `/onboarding` 라우트와 페이지를 추가한다.
6. 회원가입/로그인 후 온보딩 상태 확인 및 조건부 이동을 연결한다.
7. 마이페이지 빈 상태 안내와 온보딩 진입 버튼을 추가한다.
8. 프론트엔드 빌드와 백엔드 테스트를 실행한다.

## Validation

프론트엔드 변경 후:

```powershell
cd frontend
npm run build
```

백엔드 변경 후:

```powershell
cd backend
.\gradlew.bat test
```

## Risks And Follow-ups

- 현재 문서와 일부 소스 출력에 인코딩 깨짐이 보인다. 사용자-facing 문구를 수정할 때 실제 브라우저 표시를 확인해야 한다.
- `onboarding_completed` 컬럼 없이 완료 여부를 추론하면 기준 변경 시 동작이 흔들릴 수 있다.
- 독서 목적 저장은 현재 schema에 직접 대응되지 않으므로 1차 범위에서 제외하거나 별도 schema 변경 승인이 필요하다.
- 추천 이유 고도화는 저장 API 이후의 2차 작업으로 분리하는 것이 안전하다.
