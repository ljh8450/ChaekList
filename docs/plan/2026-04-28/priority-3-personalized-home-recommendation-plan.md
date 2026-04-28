# 다음 구현 계획: 개인화 홈 추천 실제화

## 기준

- 작성일: 2026-04-28
- 상위 계획: `docs/plan/2026-04-28/follow-up-plan.md`
- 완료된 선행 구현: `docs/plan/2026-04-28/priority-2-book-detail-interactions-plan.md`
- 대상 항목: `1순위: 개인화 홈 추천 실제화`
- 목적: 로그인 사용자의 관심 분야, 읽은 책, 저장한 책, 관심 없음 행동을 반영해 홈의 오늘의 추천을 실제 개인화 추천으로 바꾼다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 확인한 현재 코드

backend:

- 홈 API는 `backend/src/main/java/com/example/chaeklist/domain/book/controller/BookController.java`에 있다.
- 공개 홈은 `GET /api/home`, 개인화 홈은 `GET /api/me/home`이다.
- 홈 응답은 `HomeResponse`이며 필드는 `personalized`, `todayRecommendation`, `popularBooks`, `trendingBooks`, `categoryRankings`다.
- `BookService.getPersonalHome(AuthenticatedUser user)`는 현재 기본 추천과 같은 흐름에 가깝다.
- `BookSummaryResponse`에는 `recommendationReason` 필드가 있다.
- 사용자 행동은 `user_book_interactions`에 `READ`, `SAVE`, `UNSAVE`, `DISMISS` 등으로 저장된다.
- 관심 분야는 `user_interest_categories`에 저장된다.

frontend:

- 홈 화면은 `frontend/src/pages/HomePage.jsx`에서 구현되어 있다.
- 현재 홈은 mock 데이터(`frontend/src/data/books.js`)를 직접 사용한다.
- 로그인 여부는 `useAuth()`의 `currentUser`로 분기한다.
- 아직 `GET /api/home`, `GET /api/me/home` 응답을 사용하지 않는다.

현재 결론:

- 다음 구현은 backend 추천 산정과 frontend 홈 API 연결을 함께 다룬다.
- DB schema 변경 없이 기존 테이블만 사용한다.
- 추천 품질보다 "사용자 행동이 실제 응답과 화면에 반영된다"는 체감 구현을 우선한다.

## 현재 직후 구현 단위

다음 구현은 `개인화 홈 추천 실제화`를 진행한다.

우선순위는 다음과 같다.

1. backend에서 사용자별 추천 후보 점수를 계산한다.
2. `GET /api/me/home`의 `todayRecommendation`을 사용자 행동 기반 후보로 바꾼다.
3. `DISMISS`한 책은 추천 후보에서 제외한다.
4. 추천 이유를 관심 분야, 읽은 책, 저장한 책 중 실제 근거에 맞춰 생성한다.
5. frontend 홈 화면이 실제 홈 API를 호출해 추천/랭킹/급상승 데이터를 표시하게 한다.

## 구현 목표

1. 비로그인 사용자는 `GET /api/home` 기반 공개 홈을 본다.
2. 로그인 사용자는 `GET /api/me/home` 기반 개인화 홈을 본다.
3. 관심 분야가 있는 사용자는 해당 분야의 교양 도서가 추천 우선권을 가진다.
4. 읽은 책과 저장한 책의 카테고리/키워드가 추천 후보 점수에 반영된다.
5. 관심 없음으로 표시한 책은 오늘의 추천 후보에서 제외된다.
6. 추천 후보가 부족하면 기존 랭킹 기반 추천으로 fallback한다.

## 상세 구현 계획

### 1. backend 추천 후보 산정 추가

대상 파일:

- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`

작업 내용:

- `getPersonalHome(AuthenticatedUser user)`에서 기본 추천 대신 개인화 추천 후보를 계산한다.
- 후보는 `books.is_general_eligible = TRUE`인 책으로 제한한다.
- 1차 후보 수는 과도하게 넓히지 않는다.
  - 예: 최신/랭킹/카테고리 기반 후보 최대 50권
- 점수는 단순 가중치 합산으로 시작한다.

권장 점수 기준:

- 관심 분야 카테고리 일치: `+50`
- 읽은 책 카테고리와 일치: `+25`
- 저장한 책 카테고리와 일치: `+20`
- 읽은 책 키워드와 공통 키워드 1개당: `+10`
- 저장한 책 키워드와 공통 키워드 1개당: `+8`
- 이미 읽은 책: 후보 제외
- 관심 없음으로 표시한 책: 후보 제외
- 저장한 책: 제외하지 않고 "나중에 읽을 후보" 신호로 유지한다.

주의 사항:

- 협업 필터링은 제외한다.
- 새 테이블이나 schema 변경은 하지 않는다.
- SQL은 기존 `JdbcTemplate` 사용 패턴을 따른다.

### 2. 추천 이유 생성

대상 파일:

- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- 필요 시 `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookSummaryResponse.java`

작업 내용:

- 추천 후보를 선택한 근거를 `recommendationReason`에 반영한다.
- DTO 구조 변경 없이 `BookSummaryResponse.recommendationReason` 값을 개인화 문장으로 채운다.
- 기존 `BookSummaryResponse.from(Book)`만으로 부족하면 개인화 이유를 받는 factory method를 추가한다.

권장 문구:

- 관심 분야 기반: `관심 분야로 선택한 {category} 분야의 교양 도서입니다.`
- 읽은 책 기반: `읽은 책과 {keyword} 키워드를 공유합니다.`
- 저장한 책 기반: `저장한 책과 비슷한 {category} 분야의 다음 후보입니다.`
- fallback: `랭킹 지표와 교양 필터링 기준을 반영한 책입니다.`

주의 사항:

- 추천 이유는 짧고 구체적으로 쓴다.
- 근거가 없는데 개인화된 것처럼 보이는 문구를 만들지 않는다.

### 3. frontend 홈 API 연결

대상 파일:

- `frontend/src/pages/HomePage.jsx`

작업 내용:

- 로그인 전에는 `GET /api/home`을 호출한다.
- 로그인 후에는 `GET /api/me/home`을 호출하고 `Authorization` 헤더를 포함한다.
- 응답의 `todayRecommendation`, `popularBooks`, `trendingBooks`, `categoryRankings`를 화면에 표시한다.
- API 실패 시 기존 mock 데이터로 fallback한다.
- 401 응답이면 기존 인증 흐름에 맞춰 `logout()` 처리한다.

주의 사항:

- 홈 화면의 현재 디자인 구조는 유지한다.
- 새 컴포넌트는 만들지 않고 기존 `BookCard`를 우선 재사용한다.
- 로딩 중에는 카드 영역이 과하게 흔들리지 않게 간단한 로딩 문구를 표시한다.

### 4. 테스트 보강

대상 파일:

- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`

작업 내용:

- 관심 분야 기반으로 `GET /api/me/home`의 `todayRecommendation`이 바뀌는 테스트를 추가한다.
- 읽은 책 기반 키워드/카테고리 반영 테스트를 추가한다.
- `DISMISS`한 책이 추천 후보에서 제외되는 테스트를 추가한다.
- 후보가 없을 때 기존 랭킹 fallback이 동작하는지 확인한다.

frontend 검증:

- 현재 frontend 테스트 스크립트가 없으므로 `npm run build`로 검증한다.
- 수동 확인은 로그인/비로그인 홈 표시와 추천 이유 표시를 중심으로 한다.

## 제외 범위

이번 구현에서는 다음을 제외한다.

- DB schema 변경
- 외부 도서 API 연동
- 추천 히스토리 저장
- 협업 필터링
- 유사 사용자 추천
- 리뷰/감정 분석
- 독서 목적 기반 추천
- 별도 추천 설명 카드 컴포넌트 신설

## 예상 변경 파일

backend:

- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookSummaryResponse.java`
- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`

frontend:

- `frontend/src/pages/HomePage.jsx`

## 구현 순서

1. `BookService.getPersonalHome()`의 현재 기본 추천 흐름을 확인한다.
2. 사용자 관심 분야, 읽은 책, 저장한 책, 관심 없음 책 ID를 조회하는 private 메서드를 추가한다.
3. 교양 도서 후보를 조회하고 단순 점수를 계산한다.
4. 최고 점수 후보를 `todayRecommendation`으로 반환한다.
5. 추천 이유를 점수 근거에 맞춰 `BookSummaryResponse`에 넣는다.
6. 후보가 없으면 기존 `getDefaultRecommendation()`으로 fallback한다.
7. `BookControllerTest`에 개인화 추천 테스트를 추가한다.
8. `HomePage.jsx`를 실제 홈 API 응답 기반으로 전환한다.
9. backend와 frontend 검증 명령을 실행한다.

## 검증 계획

backend 변경 후:

```powershell
cd backend
.\gradlew.bat test
```

frontend 변경 후:

```powershell
cd frontend
npm run build
```

수동 확인 항목:

- 비로그인 홈에서 공개 추천과 랭킹이 표시된다.
- 로그인 홈에서 개인화 추천 문구와 추천 책이 표시된다.
- 관심 없음으로 표시한 책이 오늘의 추천으로 다시 나오지 않는다.
- API 실패 시 홈 화면이 완전히 깨지지 않고 fallback 데이터를 표시한다.
- 추천 이유가 빈 문자열로 표시되지 않는다.

## 리스크와 후속 작업

- 초기 데이터가 적으면 개인화 추천 품질이 낮을 수 있다.
- `DISMISS`는 현재 취소 흐름이 없으므로 한 번 제외한 책은 계속 제외된다.
- 추천 이유를 응답에서만 생성하면 추천 히스토리에 남지 않는다. 다음 구현 단위에서 저장 기준을 정해야 한다.
- 홈 화면이 API 기반으로 전환되면 mock 데이터와 실제 응답의 필드 차이를 더 엄격히 맞춰야 한다.
- 추천 산정 SQL이 커지면 이후 별도 recommendation service로 분리할 필요가 있다.
