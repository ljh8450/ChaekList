# SNS 기능 확장 상세 구현 계획

## 기준

- 작성일: 2026-04-29
- 상위 계획: `docs/plan/2026-04-29/follow-up-plan.md`
- 기존 참고 계획: `docs/plan/2026-04-29/sns-expansion-plan.md`
- 선행 조건: `docs/plan/2026-04-29/priority-1-reading-growth-gamification-plan.md`
- 역할: planner
- 범위: 계획 문서화만 수행한다. 코드, API, DB schema 변경은 포함하지 않는다.

## 목표

- 사용자가 선택적으로 독서 활동과 성장 카드를 공유할 수 있게 한다.
- 자유 게시글보다 구조화된 공유 카드 중심으로 시작해 moderation 부담을 낮춘다.
- 공개 범위와 개인정보 노출 정책을 먼저 확정한 뒤 feed, like, public profile을 단계적으로 구현한다.

## 선행 조건

- 검색 기반 책 추가가 안정적으로 동작해야 한다.
- 읽은 책, 저장한 책, 추천 히스토리, 독서 목적 데이터가 충분히 쌓여야 한다.
- 개인 독서 성장 카드가 먼저 구현되어 공유 가능한 콘텐츠 단위가 생겨야 한다.
- 공개/비공개 기본값과 사용자가 제어할 수 있는 공개 범위가 확정되어야 한다.

## 1차 구현 범위

- 공개 범위 정책 설계
- 구조화된 공유 게시글 저장
- 공개 피드 조회
- 좋아요/좋아요 취소
- 공개 프로필 기본 응답
- 사용자가 공유한 게시글 비공개 전환 또는 삭제

## 1차 제외 범위

- 자유 텍스트 게시글
- 댓글, 대댓글
- 팔로우, 친구 추천, DM
- 이미지 업로드 게시글
- 실시간 알림
- 공개 랭킹
- 자동 moderation

## schema 후보

구현 전 DB schema 변경 승인이 필요하다.

```sql
CREATE TABLE social_posts (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  post_type VARCHAR(50) NOT NULL,
  book_id BIGINT NULL,
  content VARCHAR(500) NULL,
  visibility VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);
```

```sql
CREATE TABLE social_post_likes (
  id BIGINT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE (post_id, user_id)
);
```

추가 후보:

- `user_public_profiles`
- `user_privacy_settings`
- `social_reports`
- `user_blocks`

## API 후보

- `GET /api/social/feed`
- `POST /api/social/posts`
- `PATCH /api/social/posts/{postId}`
- `DELETE /api/social/posts/{postId}`
- `POST /api/social/posts/{postId}/likes`
- `DELETE /api/social/posts/{postId}/likes`
- `GET /api/users/{userId}/public-profile`
- `PATCH /api/me/privacy-settings`

## backend 계획

대상 후보:

- 신규 후보: `backend/src/main/java/com/example/chaeklist/domain/social`
- `backend/src/main/java/com/example/chaeklist/domain/mypage`
- `backend/src/main/java/com/example/chaeklist/domain/book`
- `backend/src/test/java/com/example/chaeklist`

작업 순서:

1. 공개 범위 enum을 정의한다.
2. 게시글 유형 enum을 정의한다.
3. schema 변경 승인 후 social 테이블을 추가한다.
4. 공유 가능한 게시글 유형을 읽은 책, 저장한 책, 추천받은 책, 성장 카드로 제한한다.
5. 게시글 생성 시 사용자가 소유한 데이터인지 검증한다.
6. 공개 피드는 `PUBLIC` 게시글만 최신순으로 반환한다.
7. 좋아요는 중복 방지 unique key를 사용한다.
8. 비공개 전환 또는 삭제 시 feed에서 제외되는지 검증한다.
9. 공개 프로필은 닉네임, 공개 성장 요약, 공개 게시글 수 정도로 제한한다.
10. 테스트에 공개/비공개 노출, 소유권 검증, 중복 좋아요 방지, 삭제/비공개 전환 케이스를 추가한다.

게시글 유형 후보:

- `READ_BOOK`: 읽은 책 공유
- `SAVED_BOOK`: 저장한 책 공유
- `RECOMMENDED_BOOK`: 추천으로 발견한 책 공유
- `READING_GROWTH`: 독서 성장 카드 공유
- `BADGE`: 배지 획득 공유

## frontend 계획

대상 후보:

- `frontend/src/pages/MyPage.jsx`
- `frontend/src/pages/HomePage.jsx`
- 신규 후보: `frontend/src/pages/SocialFeedPage.jsx`
- 신규 후보: `frontend/src/components/SocialPostCard.jsx`
- 신규 후보: `frontend/src/components/ShareBookButton.jsx`

작업 순서:

1. 마이페이지 성장 카드에 공유 버튼을 검토한다.
2. 읽은 책/저장한 책/추천 히스토리 항목에 공유 버튼을 검토한다.
3. 공유 버튼은 기본 비공개 상태에서 명시적 공개 확인을 거치게 한다.
4. 공개 피드 페이지를 추가한다.
5. 피드 카드는 책 정보, 사용자 닉네임, 활동 유형, 좋아요 수만 표시한다.
6. 좋아요/좋아요 취소 상태를 처리한다.
7. 본인 게시글에는 비공개 전환 또는 삭제 액션을 제공한다.
8. 공개 프로필 진입은 피드 카드의 사용자 닉네임에서 연결한다.

## 공개 범위 정책

초기 기본값:

- 읽은 책 목록: 비공개
- 저장한 책 목록: 비공개
- 독서 성장 카드: 사용자가 직접 공유할 때만 공개
- 배지: 사용자가 직접 공유할 때만 공개
- 관심 분야: 비공개 또는 일부 공개를 사용자가 선택

필수 정책:

- 사용자가 공유하지 않은 독서 기록은 feed에 노출하지 않는다.
- 게시글 삭제 또는 비공개 전환이 가능해야 한다.
- 탈퇴 또는 계정 비활성화 시 공개 게시글 처리 정책을 정해야 한다.

## moderation 계획

1차에서 자유 텍스트를 제한하더라도 아래 정책은 별도 계획이 필요하다.

- 게시글 신고
- 사용자 차단
- 관리자 숨김 처리
- 부적절한 닉네임 대응
- 반복 게시 제한

신고/차단 없이 공개 feed를 열면 운영 리스크가 있으므로 SNS 1차 구현 전에 최소 정책을 확정한다.

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

수동 확인:

- 공개 피드 조회
- 공유 게시글 생성
- 비공개 게시글 미노출
- 좋아요/좋아요 취소
- 중복 좋아요 방지
- 본인 게시글 삭제 또는 비공개 전환
- 모바일 feed 레이아웃

## 주의 사항

- SNS는 schema, 개인정보, moderation 정책 변경이 필요하므로 구현 전 별도 승인이 필요하다.
- 초기 사용자가 적으면 feed가 비어 보일 수 있다.
- SNS가 추천 품질 개선보다 먼저 커지면 제품 초점이 흐려질 수 있다.
- 공개 데이터는 opt-in을 기본으로 한다.
