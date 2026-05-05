# 회원 탈퇴 기능 보강 계획

## 목적
- 현재 구현된 설정/소셜 영역에 회원 탈퇴 기능을 완성도 있게 보강한다.
- README에 명시된 정책인 `POST /api/me/withdraw`, 탈퇴 계정 로그인 차단, 기존 공개 게시글 작성자 익명화를 실제 동작과 테스트로 고정한다.
- 구현 범위는 기존 구조를 유지하며 최소 변경으로 제한한다.

## 현재 확인한 상태
- `frontend/src/pages/SettingsPage.jsx`
  - 이미 `POST /api/me/withdraw`를 호출하고 성공 시 `logout()`을 실행한다.
  - 확인창 문구와 탈퇴 섹션이 존재한다.
- `backend/src/main/java/com/example/chaeklist/domain/social/controller/SocialController.java`
  - 이미 `POST /api/me/withdraw` 엔드포인트가 존재한다.
- `backend/src/main/java/com/example/chaeklist/domain/social/service/SocialService.java`
  - `withdraw()`가 존재한다.
  - 사용자의 `social_posts`를 `user_id = NULL`, `author_snapshot_nickname = '탈퇴한 사용자'`, `author_anonymized = TRUE`로 갱신한다.
  - `users`는 `email = deleted_{id}@deleted.local`, `nickname = deleted_{id}`, `password_hash = ''`, `status = 'DELETED'`로 갱신한다.
- `docs/mysql-ddl.sql`
  - `users.status`는 `ACTIVE`, `INACTIVE`, `DELETED`를 허용한다.
  - `users.email`, `users.nickname`은 unique key가 있어 탈퇴 시 대체 값이 필요하다.
- 테스트 상태
  - `SocialControllerTest`에는 공개/비공개/비활성 계정 노출 테스트는 있으나, `withdraw` 직접 회귀 테스트는 아직 확인되지 않았다.
- 주의점
  - `TokenService.validateAccessToken()`은 JWT claim만 검증하고 DB의 최신 사용자 상태를 재조회하지 않는다.
  - 따라서 탈퇴 직후 프론트는 로그아웃되지만, 이미 발급된 access token은 만료 전까지 서버에서 `ACTIVE` claim으로 통과할 수 있다.

## 구현 정책
- 탈퇴는 soft delete로 처리한다.
- 탈퇴한 사용자는 재로그인할 수 없어야 한다.
- 탈퇴 직후 기존 access token으로 보호 API를 호출해도 401이 되어야 한다.
- 기존 공개 게시글은 삭제하지 않고 작성자 정보를 익명화해 유지한다.
- 탈퇴 사용자 공개 프로필은 조회되지 않아야 한다.
- 탈퇴 사용자가 호스트인 모각독은 기존 공개 조회 정책처럼 `host.status = 'ACTIVE'` 조건에 의해 공개 목록에서 제외되는 현 정책을 유지한다.
- DB 스키마 변경은 우선 필요하지 않은 것으로 본다.

## 구현 단위
1. `[Backend 탈퇴 인증 차단 보강]` -> verify: `POST /api/me/withdraw` 후 같은 access token으로 `/api/me/settings` 또는 `/api/me/mypage` 호출 시 401
   - `TokenService.validateAccessToken()`이 토큰의 `sub`에 해당하는 사용자를 DB에서 조회하고, 현재 `users.status = 'ACTIVE'`일 때만 `AuthenticatedUser`를 반환하도록 보강한다.
   - 토큰 claim의 status만 신뢰하지 않도록 한다.
   - 조회 대상 사용자가 없거나 `DELETED/INACTIVE`이면 `TokenException`을 던진다.

2. `[Backend 탈퇴 처리 회귀 테스트 추가]` -> verify: `cd backend && .\gradlew.bat test --tests com.example.chaeklist.SocialControllerTest`
   - 탈퇴 API 성공 시 `204 No Content`.
   - `users.status = 'DELETED'`, `email/nickname/password_hash`가 익명화/무효화되는지 검증한다.
   - 기존 공개 게시글은 남고 `user_id = NULL`, `author_anonymized = TRUE`, 표시 닉네임이 `탈퇴한 사용자`인지 검증한다.
   - 탈퇴 사용자 공개 프로필은 404 또는 공개 목록 제외로 검증한다.
   - 탈퇴 후 같은 토큰으로 보호 API 접근이 401인지 검증한다.

3. `[Frontend 탈퇴 UX 안정화]` -> verify: `cd frontend && npm run build`
   - 현재 `window.confirm` 기반 흐름은 유지하되, 요청 중 버튼 중복 클릭을 막는 pending 상태를 추가한다.
   - 401이면 기존 패턴대로 `logout()`한다.
   - 실패 메시지는 현재의 일반 메시지를 유지하거나 서버 메시지가 있으면 우선 표시한다.
   - 성공 후 사용자가 인증 상태에 남지 않도록 현재 `logout()` 흐름을 유지한다.

4. `[공개 노출 정책 점검]` -> verify: `cd backend && .\gradlew.bat test`
   - 피드/좋아요한 글/검색/공개 프로필/모각독 목록의 탈퇴 사용자 노출 정책을 기존 조건과 맞춘다.
   - 공개 게시글은 `user_id = NULL`로 남기기 때문에 피드 조건 `(sp.user_id IS NULL OR u.status = 'ACTIVE')`를 통과하는 현재 정책을 유지한다.
   - 작성자 배지/공개 프로필 링크처럼 `authorUserId`가 필요한 부가 정보는 `null` 처리로 유지한다.

## 예상 변경 파일
- `backend/src/main/java/com/example/chaeklist/domain/auth/util/TokenService.java`
- `backend/src/test/java/com/example/chaeklist/SocialControllerTest.java`
- 필요 시 `backend/src/test/java/com/example/chaeklist/AuthControllerTest.java`
- `frontend/src/pages/SettingsPage.jsx`

## DB 변경 여부
- `docs/mysql-ddl.sql` 기준으로는 추가 컬럼이나 제약 변경이 필요하지 않다.
- 이미 `users.status`에 `DELETED`가 있고, `email/nickname` unique 제약을 피하기 위한 익명화 값도 현재 구현에서 만들고 있다.
- 따라서 사용자 실행 SQL은 없을 가능성이 높다.

## 남은 확인 사항
- 탈퇴 계정의 기존 알림, 좋아요, 차단, 신고 이력은 감사/무결성 목적상 유지하는 것이 현재 구조와 가장 맞다.
- 탈퇴 사용자가 호스트였던 진행 중 모각독을 자동 취소할지 여부는 별도 정책 결정이 필요하다. 이번 계획에서는 기존 공개 목록 제외 정책만 유지한다.
- refresh token API가 현재 README/코드에 명확히 드러나지 않아, 이번 범위는 access token 검증 강화로 제한한다.
