# 다음 구현 계획: 책 상세 행동 추가

## 기준

- 작성일: 2026-04-28
- 상위 계획: `docs/plan/2026-04-28/follow-up-plan.md`
- 대상 항목: `1순위: 책 상세 행동 추가`
- 목적: 사용자가 책 상세에서 저장, 읽음 행동을 직접 남기고 이후 마이페이지와 추천에 반영할 수 있게 한다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 확인한 현재 코드

frontend:

- 책 상세 화면은 `frontend/src/pages/BookDetailPage.jsx`에서 구현되어 있다.
- 책 상세는 현재 `GET /api/books/{bookId}`를 인증 헤더 없이 호출한다.
- 인증 정보는 `frontend/src/App.jsx`의 `useAuth()`에서 `accessToken`, `currentUser`, `logout`으로 제공한다.
- 마이페이지는 `frontend/src/pages/MyPage.jsx`에서 `GET /api/me/mypage`를 호출하고 `savedBooks`, `readBooks`를 표시한다.
- 책 카드는 `frontend/src/components/BookCard.jsx`에서 공통 표시를 담당하지만 현재 사용자별 저장/읽음 상태는 받지 않는다.

backend:

- 책 상세 API는 `backend/src/main/java/com/example/chaeklist/domain/book/controller/BookController.java`의 `GET /api/books/{bookId}`다.
- 책 상세 조회 로직은 `BookService.getBookDetail(String bookId)`가 담당한다.
- 책 상세 응답은 `BookDetailResponse`이고 현재 사용자별 행동 상태 필드가 없다.
- 사용자 인증 처리 패턴은 `MyPageController.authenticate()`와 `BookController.myHome()`에 이미 있다.
- `user_book_interactions`는 별도 entity/repository 없이 `MyPageService`에서 `JdbcTemplate` SQL로 사용한다.
- 현재 코드에서 확인된 interaction type은 `READ`, `VIEW`, `CLICK`, `SAVE`, `UNSAVE`다.
- `DISMISS` 또는 `관심 없음`에 해당하는 저장 타입은 확인되지 않았다.

현재 결론:

- 이번 구현은 DB schema 변경 없이 `SAVE`, `UNSAVE`, `READ`만 1차 범위로 확정한다.
- `관심 없음`은 기존 타입이 없으므로 이번 구현에서는 버튼을 노출하지 않고 별도 후속 승인 대상으로 둔다.
- 사용자별 책 상세 상태는 `saved`, `read` boolean으로 응답에 추가하는 것이 가장 작은 변경이다.

## 현재 직후 구현 단위

다음 구현은 `책 상세 행동 추가`를 진행한다.

우선순위는 다음과 같다.

1. 책 상세 응답에 사용자별 `saved`, `read` 상태를 추가한다.
2. `POST /api/me/books/{bookId}/interactions`를 추가해 `SAVE`, `UNSAVE`, `READ`를 기록한다.
3. 책 상세 화면에 `저장하기/저장됨`, `읽었어요/읽은 책` 버튼을 추가한다.
4. 행동 결과가 마이페이지의 저장한 책, 읽은 책 상태와 어긋나지 않게 확인한다.
5. `관심 없음`은 schema 변경 승인 전까지 제외한다.

## 구현 목표

1. 로그인 사용자는 책 상세에서 저장하기를 수행할 수 있다.
2. 로그인 사용자는 저장한 책을 책 상세에서 저장 취소할 수 있다.
3. 로그인 사용자는 책 상세에서 읽었어요를 수행할 수 있다.
4. 이미 수행한 행동은 현재 상태로 표시한다.
5. 비로그인 사용자는 책 상세 조회는 가능하지만 행동 버튼 사용 시 로그인 흐름으로 이동한다.

## 상세 구현 계획

### 1. backend 책 상세 응답 확장

대상 파일:

- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookDetailResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/controller/BookController.java`

작업 내용:

- `BookDetailResponse`에 사용자 행동 상태 필드를 추가한다.
  - `boolean saved`
  - `boolean read`
- 인증 헤더가 없는 공개 책 상세 호출은 계속 동작해야 한다.
  - `saved=false`
  - `read=false`
- 인증 헤더가 있으면 token을 검증하고 해당 사용자의 최신 행동 상태를 조회한다.
- invalid token은 기존 개인화 API와 동일하게 401로 처리한다.

권장 controller 흐름:

1. `GET /api/books/{bookId}`에서 `Authorization` 헤더를 선택적으로 받는다.
2. 헤더가 없으면 기존 공개 상세 조회를 유지한다.
3. 헤더가 있으면 `AuthenticatedUser`를 만든 뒤 사용자 상태 포함 상세 조회를 호출한다.

주의 사항:

- `GET /api/books/{bookId}`를 인증 필수 API로 바꾸지 않는다.
- 기존 frontend fallback 동작이 깨지지 않게 응답 필드는 추가만 한다.

### 2. backend interaction 저장 API 추가

대상 파일:

- `backend/src/main/java/com/example/chaeklist/domain/mypage/controller/MyPageController.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/BookInteractionRequest.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/BookInteractionResponse.java`

API:

- `POST /api/me/books/{bookId}/interactions`

요청:

```json
{ "type": "SAVE" }
```

```json
{ "type": "UNSAVE" }
```

```json
{ "type": "READ" }
```

응답:

```json
{
  "bookId": "301",
  "saved": true,
  "read": false
}
```

처리 기준:

- `bookId`는 숫자로 파싱하고, `books.is_general_eligible = TRUE`인 책만 허용한다.
- 허용 type은 `SAVE`, `UNSAVE`, `READ`만 둔다.
- `SAVE`는 저장 이벤트를 추가한다.
- `UNSAVE`는 저장 취소 이벤트를 추가한다.
- `READ`는 읽음 이벤트를 추가한다.
- 저장 상태는 기존 `getSavedBooks()`처럼 최신 `SAVE` 이후의 `UNSAVE` 존재 여부로 판단한다.
- 읽음 상태는 사용자/책에 `READ`가 하나라도 있으면 true로 판단한다.

중복 처리:

- 이벤트 로그 구조를 유지하되 같은 상태를 반복 요청할 때 불필요한 row를 늘리지 않는다.
- 이미 저장된 책에 `SAVE`가 오면 새 row를 만들지 않고 현재 상태를 반환한다.
- 저장되지 않은 책에 `UNSAVE`가 오면 새 row를 만들지 않고 현재 상태를 반환한다.
- 이미 읽은 책에 `READ`가 오면 새 row를 만들지 않고 현재 상태를 반환한다.

오류 처리:

- 인증 헤더 누락 또는 invalid token: 401
- 없는 책, 숫자가 아닌 책 ID, 교양 대상이 아닌 책: 404 또는 기존 책 상세 정책에 맞춘 오류
- 미지원 type: 400
- `DISMISS`는 1차 구현에서 400으로 응답한다.

### 3. frontend 책 상세 UI 연결

대상 파일:

- `frontend/src/pages/BookDetailPage.jsx`

작업 내용:

- `useAuth()`를 사용해 `accessToken`, `currentUser`, `logout`을 가져온다.
- 책 상세 조회 시 accessToken이 있으면 `Authorization: Bearer ...` 헤더를 추가한다.
- `book.saved`, `book.read` 값을 버튼 상태에 반영한다.
- 책 상세 정보 영역의 조회/저장 수 아래 또는 추천 이유 위에 행동 버튼 묶음을 배치한다.
- `저장하기` 버튼은 저장 전 `SAVE`, 저장 후 `UNSAVE`를 호출한다.
- `읽었어요` 버튼은 읽지 않은 상태에서만 `READ`를 호출한다.
- 요청 중에는 해당 버튼을 비활성화한다.
- 401 응답이면 기존 인증 패턴에 맞춰 `logout()` 처리한다.
- 실패 시 책 상세 화면의 기존 `errorMessage` 흐름을 재사용하거나 버튼 근처에 짧게 표시한다.

권장 버튼 상태:

- 저장 전: `저장하기`
- 저장 후: `저장됨`
- 읽음 전: `읽었어요`
- 읽음 후: `읽은 책`

비로그인 사용자:

- 책 상세 조회는 기존처럼 가능하다.
- 행동 버튼 클릭 시 `/login`으로 이동한다.
- 기존 라우팅 패턴을 고려해 `useNavigate()`로 이동하는 방식이 작다.

제외:

- `관심 없음` 버튼은 1차에서 노출하지 않는다.
- 모든 `BookCard`에 상태 뱃지를 추가하지 않는다.

### 4. 마이페이지 반영 확인

대상 파일:

- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `frontend/src/pages/MyPage.jsx`

작업 내용:

- 저장 후 `GET /api/me/mypage`의 `savedBooks`에 해당 책이 포함되는지 확인한다.
- 저장 취소 후 `savedBooks`에서 빠지는지 확인한다.
- 읽음 처리 후 `readBooks`에 해당 책이 포함되는지 확인한다.
- 마이페이지 UI는 이미 `savedBooks`, `readBooks`를 표시하므로 1차 구현에서는 변경하지 않는다.

주의 사항:

- `getBooksByInteraction()`은 `READ` 중복 row가 있어도 `GROUP BY`로 한 번만 보인다.
- `getSavedBooks()`는 `UNSAVE` 이벤트를 고려하므로 저장 취소 API는 기존 쿼리와 맞는다.

## 제외 범위

이번 구현에서는 다음을 제외한다.

- DB schema 변경
- 외부 도서 API 연동
- 검색 기반 책 추가
- 추천 알고리즘 변경
- 추천 히스토리 저장 로직 변경
- 독서 목적 저장
- 관심 없음 타입을 위한 무승인 schema 확장
- 책 카드 전체에 사용자 행동 상태 뱃지 표시

## 예상 변경 파일

frontend:

- `frontend/src/pages/BookDetailPage.jsx`

backend:

- `backend/src/main/java/com/example/chaeklist/domain/book/controller/BookController.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookDetailResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/controller/MyPageController.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/BookInteractionRequest.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/BookInteractionResponse.java`

tests:

- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`
- `backend/src/test/java/com/example/chaeklist/MyPageControllerTest.java`

## 구현 순서

1. `BookDetailResponse`에 `saved`, `read` 필드를 추가한다.
2. `BookService`에 사용자별 책 상태 조회 로직을 추가한다.
3. `BookController`가 `Authorization` 헤더를 선택적으로 받아 공개/인증 상세 조회를 모두 처리하게 한다.
4. `BookInteractionRequest`, `BookInteractionResponse` DTO를 추가한다.
5. `MyPageService`에 `SAVE`, `UNSAVE`, `READ` 기록 메서드와 현재 상태 조회 메서드를 추가한다.
6. `MyPageController`에 `POST /api/me/books/{bookId}/interactions`를 추가한다.
7. `BookControllerTest`에 인증 책 상세 상태 응답 테스트를 추가한다.
8. `MyPageControllerTest`에 저장, 저장 취소, 읽음, 미지원 타입 거부 테스트를 추가한다.
9. `BookDetailPage.jsx`에서 인증 헤더 포함 조회와 행동 버튼을 연결한다.
10. 로그인/비로그인 상태, API 실패, 401 처리, 중복 클릭 방지를 확인한다.
11. frontend/backend 검증 명령을 실행한다.

## 검증 계획

frontend와 backend를 함께 변경할 가능성이 높으므로 구현 후 다음 명령을 모두 실행한다.

```powershell
cd frontend
npm run build
```

```powershell
cd backend
.\gradlew.bat test
```

수동 확인 항목:

- 비로그인 상태에서도 책 상세 조회가 가능하다.
- 로그인 상태에서 책 상세 조회 시 저장/읽음 상태가 표시된다.
- 책 상세에서 저장하기를 누르면 저장 상태가 표시된다.
- 저장된 책에서 저장됨을 다시 누르면 저장이 취소된다.
- 저장한 책이 마이페이지 저장한 책 목록에 반영된다.
- 책 상세에서 읽었어요를 누르면 읽은 책 상태가 표시된다.
- 읽은 책이 마이페이지 읽은 책 목록에 반영된다.
- 같은 버튼을 여러 번 눌러도 중복 데이터가 생기지 않는다.
- 인증되지 않은 사용자가 행동 버튼을 누르면 로그인 흐름으로 이동한다.
- `관심 없음` 버튼은 아직 표시되지 않는다.

## 리스크와 후속 작업

- `관심 없음`은 기존 schema에 없으므로 별도 승인 없이는 제외한다.
- 책 상세 응답이 사용자 상태를 담게 되므로 인증 헤더가 invalid인 경우 공개 조회로 fallback하지 않고 401 처리하는 기준을 명확히 유지해야 한다.
- 저장 취소는 `UNSAVE` 이벤트 방식으로 진행한다. 이후 실제 삭제 방식이 필요하면 별도 API 설계가 필요하다.
- `READ`는 현재 온보딩 저장 시 전체 교체 방식으로도 기록되므로, 책 상세에서 추가한 `READ`와 온보딩 수정 흐름의 관계를 후속으로 점검해야 한다.
- 이 작업이 끝나면 다음 구현 단위는 `개인화 홈 추천 실제화`가 된다.
