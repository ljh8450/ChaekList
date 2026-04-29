# architecture.md

## High-level structure

- `frontend/`: React 사용자 웹 UI
- `backend/`: Spring Boot REST API와 비즈니스 로직
- `docs/`: 명령, 아키텍처, 구현 계획 문서

## Frontend structure

- `frontend/src/App.jsx`: 라우팅, 인증 context, access token 보관, 대표 배지 갱신
- `frontend/src/pages/`: 화면 단위 페이지
  - `HomePage.jsx`: 공개/개인화 홈, 키워드 트렌드
  - `RankingsPage.jsx`: 전체/카테고리 랭킹
  - `CategoriesPage.jsx`: 카테고리별 랭킹
  - `BookDetailPage.jsx`: 책 상세, 저장/읽음/관심 없음 액션
  - `OnboardingPage.jsx`: 관심 분야, 독서 목적, 읽은 책 선택
  - `MyPage.jsx`: 프로필, 독서 성장, 관심 분야, 독서 목적, 읽은 책, 저장한 책, 추천 히스토리
  - `LoginPage.jsx`, `SignupPage.jsx`: 인증 진입
- `frontend/src/components/`: 재사용 UI
  - `Header.jsx`: 전역 네비게이션, 검색 input, 사용자 메뉴, 대표 배지
  - `BookCard.jsx`: 책 카드
  - `BookSearchPanel.jsx`: 내부 DB 기반 책 검색과 선택
  - `AuthForm.jsx`: 로그인/회원가입 폼
- `frontend/src/data/`: API 실패 또는 개발용 fallback 데이터
- `frontend/src/styles/`: 전역 스타일

## Backend structure

- `backend/src/main/java/com/example/chaeklist/domain/auth`
  - 회원가입, 로그인, 현재 사용자 조회
  - token 발급/검증
- `backend/src/main/java/com/example/chaeklist/domain/book`
  - 홈, 개인화 홈, 랭킹, 급상승, 키워드 트렌드, 책 검색, 책 상세
  - `controller`, `dto`, `entity`, `repository`, `service` 계층 사용
  - Kakao 도서 검색은 현재 표지 이미지 보강에서 사용
- `backend/src/main/java/com/example/chaeklist/domain/mypage`
  - 온보딩, 마이페이지, 책 상호작용, 독서 목적, 독서 성장 계산
  - `controller`, `dto`, `model`, `service` 계층 사용
  - `user_book_interactions`, `recommendations`, `user_reading_purposes` 기반 계산 로직 포함
- `backend/src/main/java/com/example/chaeklist/global`
  - `auth`: 인증 사용자 모델과 Bearer token 해석
  - `config`: 전역 설정
  - `health`: health check API

## Current API areas

### Auth

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Books

- `GET /api/home`
- `GET /api/me/home`
- `GET /api/books/rankings`
- `GET /api/books/trending`
- `GET /api/books/trends/keywords`
- `GET /api/books/categories`
- `GET /api/books/search`
- `GET /api/books/categories/{category}/rankings`
- `GET /api/books/{bookId}`
- `POST /api/books/images/enrich`

### My Page

- `GET /api/me/mypage`
- `GET /api/me/reading-growth/primary-badge`
- `GET /api/me/onboarding-status`
- `GET /api/me/onboarding-options`
- `POST /api/me/onboarding`
- `POST /api/me/books/{bookId}/interactions`

### Health

- `GET /api/health`

## Data and behavior notes

- 책 검색은 현재 내부 DB의 교양 필터 통과 책을 제목/저자로 검색한다.
- 온보딩과 마이페이지의 책 추가 UI는 `BookSearchPanel`에서 같은 검색 API를 사용한다.
- 개인화 추천은 관심 분야, 읽은 책, 저장한 책, 독서 목적의 카테고리/키워드 기반 규칙 점수로 계산한다.
- `READ`, `DISMISS` 책은 개인 추천 후보에서 제외된다.
- 추천 결과는 `recommendations`에 `CONTENT_BASED` 유형으로 저장된다.
- 독서 목적은 enum 성격의 `ReadingPurpose` model과 `user_reading_purposes` 저장 데이터를 함께 사용한다.
- 독서 성장은 별도 영구 스냅샷 없이 현재 데이터에서 계산한다.
- 대표 배지는 Header에서 별도 API로 갱신하고, 마이페이지 로드 시에도 최신 상태로 맞춘다.

## Planned extension boundaries

### Header 통합 검색

- 신규 검색 결과 페이지와 API가 필요하다.
- 기존 `GET /api/books/search`는 책 추가용 제목/저자 검색으로 유지한다.
- 통합 검색은 책/저자/키워드/공개 게시물/공개 유저를 다루되, 비공개 독서 기록과 비공개 프로필을 노출하지 않는다.
- backend에는 필요 시 `domain/search`를 추가한다.

### SNS 기능

- 신규 backend 후보: `domain/social`
- 신규 frontend 후보: `SocialFeedPage`, `SocialPostCard`, `ShareBookButton`
- 공개 피드, 구조화된 공유 게시글, 좋아요, 공개 프로필을 1차 범위로 둔다.
- 자유 텍스트 게시글, 댓글, 팔로우, DM, 이미지 업로드, 실시간 알림은 1차에서 제외한다.
- SNS schema, 개인정보 공개 범위, moderation 정책은 구현 전 별도 승인이 필요하다.

### External book search

- Kakao 도서 검색은 현재 표지 보강에만 사용한다.
- 로컬 DB에 없는 책을 검색해 선택 저장하는 흐름은 후속 확장이다.
- 외부 결과 저장 시점, ISBN 중복 기준, 신규 책의 교양 필터 초기값을 먼저 결정해야 한다.

## Change boundaries

- UI, components, pages, client routing, API calls -> `frontend/`
- controllers, services, persistence, auth, business rules -> `backend/`
- 명령, 아키텍처, 계획, README -> `docs/`, `README.md`, `AGENTS.md`
- DB schema, 환경변수, 의존성, CI/deployment 변경은 작업 전 승인받는다.
- frontend/backend를 모두 건드리는 기능은 먼저 짧은 계획으로 API 계약과 화면 흐름을 맞춘다.

## Validation expectations

- frontend 변경 후:

```powershell
cd frontend
npm run build
```

- backend 변경 후:

```powershell
cd backend
.\gradlew.bat test
```

- 문서만 변경할 때는 빌드가 필요하지 않으며, 링크와 변경 내용을 확인한다.

## General expectations

- Reuse existing patterns first.
- Keep interfaces stable unless explicitly asked to change them.
- Avoid coupling unrelated frontend and backend changes in one edit unless required.
- Public data must be opt-in by default.
- Do not expose email, private reading history, or private profile data through search or SNS APIs.
