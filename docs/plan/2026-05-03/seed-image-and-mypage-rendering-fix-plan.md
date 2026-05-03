# Seed 이미지 및 MyPage 렌더링 문제 해결 계획

## 문제

Seed book cover 이미지가 callback 형식의 외부 이미지 URL로 표시되어 `frontend/public/book-covers`의 로컬 asset으로 우회했다. 이후 수동으로 수정했을 때 데이터 연동이 끊기고 MyPage 렌더링 오류가 발생했다.

## 전제

- seed cover의 목표 경로는 기존 public asset 경로 형식인 `/book-covers/{name}.svg`다.
- 책 데이터의 기준은 backend이며, `cover_image_url`도 backend가 관리한다.
- frontend는 기존 API 필드인 `imageUrl`을 사용하고, seed book 전용 매핑을 따로 들고 있지 않는다.
- database schema, dependency, environment variable 변경은 계획하지 않는다.

## 현재 파일 확인 결과

- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookImagePredeployRunner.java`는 알려진 seed `source_book_id`를 `/book-covers/*.svg`로 매핑한다. 다만 `cover_image_url`이 `NULL` 또는 빈 문자열일 때만 갱신한다.
- `frontend/src/components/BookCard.jsx`는 `book.imageUrl`만 렌더링한다. backend 데이터가 이 계약과 다르게 바뀌면 표지가 일관되게 표시되지 않는다.
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/MyPageBookResponse.java`는 표지를 `imageUrl`로 노출하며, frontend 기대 필드와 일치한다.
- `frontend/src/pages/MyPage.jsx`에는 여러 visible text와 prop 위치에 깨진 JSX/string literal이 있어, 데이터 문제와 별개로 render 또는 build 실패를 일으킬 수 있다.
- 기존 backend test는 MyPage 데이터 형태와 interaction을 검증한다. seed cover 전파 로직을 바꾸면 관련 검증을 보강해야 한다.

## 성공 기준

- seed book이 API 응답에서 안정적인 local cover path를 `imageUrl`로 반환한다.
- MyPage가 `/api/me/mypage`를 불러오고, JSX/runtime 오류 없이 렌더링된다.
- read/saved/recommendation 데이터가 interaction 이후에도 MyPage에서 동기화된다.
- 수동 database 수정은 불필요하거나, 기존의 잘못된 `cover_image_url` 보정으로만 제한된다.
- frontend build가 통과한다.
- backend logic을 변경한 경우 MyPage/book 관련 test가 통과한다.

## 계획

1. 오류 재현 및 범위 분리 -> verify: `frontend/`에서 `npm run build`를 실행해 MyPage syntax/render 오류를 정확히 확인한다.
2. backend API 계약 확인 -> verify: `GET /api/me/mypage`와 book summary/detail 응답을 확인해 seed cover가 `coverImageUrl`이나 callback payload가 아니라 `imageUrl`로 내려오는지 검증한다.
3. seed cover source 보수적 수정 -> verify: 필요한 경우 기존 seed cover URL 적용 조건만 조정해, 잘못된 callback-style URL이 들어간 알려진 seed row를 `/book-covers/*.svg`로 보정한다. book ID나 관계 table은 변경하지 않는다.
4. MyPage 렌더링 복구 -> verify: `frontend/src/pages/MyPage.jsx`에서 깨진 JSX/text/prop literal만 수정하고, 기존 state 흐름과 API 호출은 유지한다.
5. interaction 이후 데이터 연동 검증 -> verify: `POST /api/me/books/{bookId}/interactions` 이후 `GET /api/me/mypage`를 확인해 선택한 책이 `readBooks` 또는 `savedBooks`에 `imageUrl`과 함께 나타나는지 검증한다.
6. 필수 검증 실행 -> verify: `frontend/`에서 `npm run build`를 실행한다. backend 변경이 있으면 `backend/`에서 `.\gradlew.bat test`를 실행한다.

## 리스크

- 수동 수정이 `cover_image_url`만이 아니라 `books.id`를 바꿨다면 `book_categories`, `user_book_interactions`, `recommendations` 같은 관계 table join이 깨졌을 수 있다. 이 경우 ID를 복구하고 `cover_image_url`만 갱신해야 한다.
- 현재 seed runner는 빈 image field만 갱신하므로, 이미 callback-style URL이 들어간 row는 조건을 넓히지 않으면 자동 복구되지 않는다.
- MyPage 파일은 JSX 손상뿐 아니라 text encoding 손상도 보인다. 가장 작은 수정은 build와 render에 필요한 깨진 literal/tag만 복구하는 것이다.
