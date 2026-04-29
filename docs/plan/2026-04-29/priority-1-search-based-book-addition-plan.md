# 후속 구현 계획: 검색 기반 책 추가

## 기준

- 작성일: 2026-04-29
- 상위 계획: `docs/plan/2026-04-29/follow-up-plan.md`
- 대상 항목: `1순위: 검색 기반 책 추가`
- README 기준: 사용자가 읽을 책을 찾는 시간을 줄이고, 교양 독서에 적합한 책을 빠르게 발견하도록 돕는다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 현재 확인한 구조

- 책 API는 `BookController`와 `BookService`가 홈, 랭킹, 급상승, 카테고리, 상세 조회를 담당한다.
- `BookRepository`에는 `findByGeneralEligibleTrue`, `findByIdAndGeneralEligibleTrue`, 카테고리 기반 조회가 있다.
- 온보딩은 `/api/me/onboarding-options`에서 받은 책 목록 중 읽은 책을 선택한다.
- 온보딩 저장 요청은 `OnboardingRequest(categoryIds, readBookIds)` 구조다.
- 책 행동 저장은 `/api/me/books/{bookId}/interactions`에서 `SAVE`, `UNSAVE`, `READ`, `DISMISS`를 처리한다.
- 마이페이지는 읽은 책과 저장한 책을 조회하지만, 직접 검색해 추가하는 UI는 없다.

## 목표

- 사용자가 온보딩과 마이페이지에서 책을 검색해 읽은 책 또는 저장한 책으로 추가할 수 있게 한다.
- 외부 도서 API 없이 내부 DB 검색으로 시작한다.
- 검색 결과 없음, 짧은 검색어, 인증 만료, 중복 선택 상태를 명확히 처리한다.

## 1차 구현 범위

- 내부 DB 기반 책 검색 API
- 온보딩 읽은 책 검색 입력
- 마이페이지 읽은 책 추가 검색 입력
- 마이페이지 저장한 책 추가 검색 입력
- 검색 결과 선택 후 기존 interaction 저장 흐름 연결
- 검색 결과 없음과 오류 상태 UI

1차 제외:

- 외부 도서 API 연동
- 검색 결과를 DB에 새로 수집하는 기능
- 자동완성 품질 고도화
- 전체 검색 페이지 신설
- 비로그인 검색 저장 기능

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/book/controller/BookController.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/repository/BookRepository.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookSummaryResponse.java`
- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`

API 후보:

```http
GET /api/books/search?query=투자&limit=10
```

응답 후보:

```json
[
  {
    "id": "301",
    "title": "조용한 투자 습관",
    "author": "서도현",
    "imageUrl": null,
    "category": "경제",
    "tag": "교양 필터 통과",
    "recommendationReason": "투자 키워드와 관련된 경제 분야 교양 도서입니다."
  }
]
```

작업 순서:

1. `BookController`에 `/api/books/search` 엔드포인트를 추가한다.
2. `BookService.searchBooks(query, limit)`를 추가한다.
3. `query`는 trim 후 최소 길이 정책을 정한다.
4. `limit`은 기본 10, 최대 20으로 제한한다.
5. 검색 대상은 `title`, `author`를 1차로 두고, 가능하면 category/keyword까지 확장한다.
6. `generalEligible=true` 책만 반환한다.
7. 응답은 `BookSummaryResponse`를 재사용한다.
8. 빈 결과는 200과 빈 배열로 반환한다.
9. 너무 짧거나 비어 있는 query는 기존 요청 검증 스타일에 맞춰 400 처리한다.
10. 검색 API 테스트를 추가한다.

검색 조건 후보:

- 1차 안전안: 제목 또는 저자 부분 일치
- 1.5차 확장안: 카테고리명 부분 일치
- 2차 확장안: 키워드명 부분 일치

주의 사항:

- ManyToMany keyword/category 검색은 중복 row가 생길 수 있으므로 distinct 처리가 필요하다.
- 빈 query를 전체 책 목록처럼 쓰면 홈/랭킹 API와 역할이 겹친다.
- 검색 API는 공개 조회로 시작하되, 저장/읽음 추가는 기존 인증 API를 사용한다.

## frontend 구현 계획

대상 후보:

- `frontend/src/pages/OnboardingPage.jsx`
- `frontend/src/pages/MyPage.jsx`
- `frontend/src/components/BookCard.jsx`

온보딩 작업:

1. 읽은 책 섹션 상단에 검색 입력을 추가한다.
2. 사용자가 검색어를 입력하면 `/api/books/search`를 호출한다.
3. 검색 결과를 현재 `availableBooks` 목록과 합쳐 선택 가능하게 한다.
4. 이미 선택된 책은 선택 상태를 유지한다.
5. 검색 결과가 없으면 "검색 결과가 없습니다" 문구를 표시한다.
6. 저장 시 기존 `readBookIds`에 검색으로 선택한 책 ID도 포함한다.

마이페이지 작업:

1. 읽은 책 섹션에 "읽은 책 추가" 검색 UI를 추가한다.
2. 저장한 책 섹션에 "저장한 책 추가" 검색 UI를 추가한다.
3. 결과 선택 시 `/api/me/books/{bookId}/interactions`를 호출한다.
4. 읽은 책 추가는 `READ`, 저장한 책 추가는 `SAVE`를 사용한다.
5. 성공 후 마이페이지 데이터를 재조회하거나 로컬 상태를 갱신한다.
6. 이미 읽은 책 또는 저장한 책은 중복 선택을 막는다.

UI 기준:

- 기존 카드형 화면 안에 조밀한 검색 결과 목록을 배치한다.
- 검색 결과 항목은 제목, 저자, 카테고리, 짧은 추천 이유 정도만 표시한다.
- 모바일에서 입력창과 결과 목록이 기존 카드 그리드와 겹치지 않게 한다.

## 검증 계획

backend:

```powershell
cd backend
.\gradlew.bat test
```

frontend:

```powershell
cd frontend
npm run build
```

수동 확인:

- 제목 검색 성공
- 저자 검색 성공
- 짧은 검색어 처리
- 검색 결과 없음
- 온보딩 검색 결과 선택 후 저장
- 마이페이지 읽은 책 추가
- 마이페이지 저장한 책 추가
- 중복 선택 방지
- 인증 만료 시 로그인 흐름

## 리스크와 후속 작업

- 검색 정확도는 초기에는 낮을 수 있다.
- keyword/category 검색을 포함하면 query 복잡도와 중복 제거가 늘어난다.
- 외부 도서 API 연동은 별도 API key와 저장 정책이 필요하므로 후속 계획으로 분리한다.
- 검색 UI가 온보딩 화면을 무겁게 만들 수 있으므로 1차는 단순 목록형으로 제한한다.
