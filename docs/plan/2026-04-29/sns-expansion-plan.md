# SNS 기능 확장 계획

## 기준

- 작성일: 2026-04-29
- 기준 문서:
  - `README.md`
  - `docs/plan/2026-04-29/mvp-gap-and-expansion-check-plan.md`
  - `docs/plan/2026-04-29/gamification-expansion-plan.md`
- 목적: ChaekList의 장기 확장 방향 중 SNS 기능의 범위, 선행 조건, 구현 순서를 정리한다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 우선순위

SNS 기능은 현재 계획 중 가장 후순위로 둔다.

현재 권장 순서:

1. 카카오 책 검색 기반 책 추가
2. 독서 목적 저장과 추천 반영
3. 데이터 기반 추천 산식 정리
4. 게이미피케이션 확장
5. SNS 기능 확장

SNS를 후순위로 두는 이유:

- ChaekList의 1차 핵심 가치는 "읽을 책을 빠르게 찾는 것"이다.
- SNS는 사용자 수와 콘텐츠 생산량이 충분해야 가치가 생긴다.
- 초기에는 게시글/좋아요 기능을 만들어도 피드가 비어 보일 수 있다.
- SNS는 moderation, 신고, 차단, 알림, 공개 범위, 개인정보 설정이 함께 필요하다.
- 현재 데이터 구조는 개인 추천/독서 기록 중심이므로 먼저 개인 기록과 성장 데이터가 쌓이는 것이 안전하다.

## 목표

SNS는 독서 기록과 추천 경험을 다른 사용자와 공유하는 후속 확장으로 본다.

목표:

- 사용자가 읽은 책, 저장한 책, 추천받은 책, 독서 성장 기록을 선택적으로 공유한다.
- 다른 사용자의 독서 기록을 통해 책 발견 경로를 넓힌다.
- 단순 게시판보다 "책 기반 피드"를 만든다.
- 추천 시스템과 충돌하지 않고 보조 탐색 채널로 동작한다.

## 선행 조건

SNS 구현 전에 다음 기능이 먼저 안정화되어야 한다.

- 책 검색 기반 추가 기능
- 읽은 책/저장한 책 기록
- 독서 목적 저장
- 추천 히스토리
- 개인 성장/배지/리포트
- 공개 가능한 사용자 프로필 기본 구조

선행 데이터가 충분해야 SNS 피드에 올릴 만한 소재가 생긴다.

## 1차 SNS 범위

### 공개 독서 활동 피드

게시글 자유 작성보다 책 기반 활동 피드를 먼저 검토한다.

피드 항목 후보:

- 사용자가 책을 읽은 책으로 표시함
- 사용자가 책을 저장함
- 사용자가 추천받은 책을 읽음으로 전환함
- 사용자가 배지를 획득함
- 사용자가 월간 독서 리포트를 공유함

주의:

- 모든 활동은 기본 비공개 또는 명시적 공개 설정을 둔다.
- 사용자가 원하지 않는 독서 기록이 공개되지 않도록 해야 한다.

### 독서 기록 공유

사용자가 직접 공유할 수 있는 카드형 콘텐츠를 제공한다.

공유 후보:

- 이 책을 읽었어요
- 이 책을 저장했어요
- 이번 달 독서 성장 카드
- 새로 발견한 분야
- 추천으로 발견한 책

주의:

- 자유 텍스트 게시글보다 구조화된 공유 카드가 moderation 부담이 적다.
- 공유 단위는 책 또는 성장 카드로 제한해 초기 품질을 관리한다.

### 좋아요 기능

좋아요는 1차 SNS에서 가장 작은 상호작용으로 검토한다.

대상 후보:

- 공개 독서 활동
- 공유된 독서 성장 카드
- 공유된 책 기록

주의:

- 좋아요 수를 과도하게 강조하지 않는다.
- 개인 독서 기록이 경쟁처럼 보이지 않도록 한다.
- 좋아요 취소, 중복 방지, 인증 사용자 제한이 필요하다.

### 공개 프로필

피드와 좋아요를 붙이려면 공개 프로필이 필요하다.

표시 후보:

- 닉네임
- 공개 독서 성장 요약
- 공개 배지
- 최근 공개한 책 기록
- 관심 분야 일부

주의:

- 이메일, 로그인 정보, 비공개 독서 기록은 노출하지 않는다.
- 공개/비공개 설정을 사용자가 제어할 수 있어야 한다.

## 1차 제외 범위

- 댓글
- 대댓글
- 팔로우/팔로잉
- DM
- 전체 자유 게시판
- 이미지 업로드 게시글
- 해시태그 기반 탐색
- 실시간 알림
- 신고/제재 자동화
- 공개 랭킹
- 친구 추천

이 항목들은 moderation과 운영 비용이 커서 SNS 2차 이후로 둔다.

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/social`
- `backend/src/main/java/com/example/chaeklist/domain/mypage`
- `backend/src/main/java/com/example/chaeklist/domain/book`
- `backend/src/test/java/com/example/chaeklist`

필요 schema 후보:

- `social_posts`
- `social_post_likes`
- `user_public_profiles`
- `user_privacy_settings`

`social_posts` 후보:

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

`social_post_likes` 후보:

```sql
CREATE TABLE social_post_likes (
  id BIGINT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE (post_id, user_id)
);
```

API 후보:

- `GET /api/social/feed`
- `POST /api/social/posts`
- `POST /api/social/posts/{postId}/likes`
- `DELETE /api/social/posts/{postId}/likes`
- `GET /api/users/{userId}/public-profile`
- `PATCH /api/me/privacy-settings`

작업 순서:

1. 공개 범위 정책을 먼저 정의한다.
2. 공개 프로필 응답 구조를 설계한다.
3. 구조화된 공유 게시글 타입을 정의한다.
4. 게시글 작성 API를 추가한다.
5. 피드 조회 API를 추가한다.
6. 좋아요/좋아요 취소 API를 추가한다.
7. 내 비공개 기록이 피드에 노출되지 않는지 테스트한다.
8. 중복 좋아요 방지 테스트를 추가한다.

## frontend 구현 계획

대상 후보:

- `frontend/src/pages/MyPage.jsx`
- `frontend/src/pages/HomePage.jsx`
- 신규 후보: `frontend/src/pages/SocialFeedPage.jsx`
- 신규 후보: `frontend/src/components/SocialPostCard.jsx`

작업 순서:

1. 마이페이지에서 독서 성장 카드 공유 버튼을 검토한다.
2. 책 상세 또는 읽은 책 목록에서 "공유하기" 버튼을 검토한다.
3. 공개 피드 페이지를 추가한다.
4. 피드 카드는 책 정보, 사용자 닉네임, 활동 타입, 좋아요 버튼만 표시한다.
5. 좋아요 상태와 count를 표시한다.
6. 비공개 상태 또는 공유 취소 상태를 처리한다.

UI 기준:

- 피드는 책 발견을 돕는 정보 중심이어야 한다.
- 게시글 텍스트보다 책 카드와 활동 맥락을 우선한다.
- 좋아요 수는 작게 표시한다.
- 경쟁적인 랭킹 UI는 피한다.

## 공개 범위 정책

초기 기본값:

- 읽은 책 기록: 비공개
- 저장한 책 기록: 비공개
- 성장 카드: 사용자가 직접 공유할 때만 공개
- 배지: 사용자가 공개 허용 시 공개
- 관심 분야: 일부 공개 또는 비공개 선택

필수 정책:

- 사용자가 공유하지 않은 독서 기록은 피드에 노출하지 않는다.
- 공유 게시글 삭제 또는 비공개 전환이 가능해야 한다.
- 탈퇴 또는 계정 비활성화 시 공개 게시글 처리 정책이 필요하다.

## moderation 고려사항

1차에서 자유 텍스트를 최소화하면 moderation 부담을 줄일 수 있다.

그래도 필요한 항목:

- 게시글 신고
- 사용자 차단
- 관리자 숨김 처리
- 부적절한 닉네임 대응
- 스팸성 반복 게시 제한

주의:

- 신고/차단이 없으면 공개 SNS 기능은 운영 리스크가 크다.
- 따라서 자유 게시글과 댓글은 신고/차단 체계가 생긴 뒤로 미룬다.

## 게이미피케이션과의 연결

SNS는 게이미피케이션 이후에 붙이는 것이 자연스럽다.

연결 후보:

- 독서 성장 카드 공유
- 배지 획득 공유
- 월간 리포트 공유
- 추천으로 발견한 책 공유
- 공개 프로필의 성장 요약 표시

주의:

- 성장 점수나 레벨을 공개 경쟁으로 만들지 않는다.
- 공유는 opt-in으로 둔다.

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
- 게시글 공유
- 좋아요/좋아요 취소
- 중복 좋아요 방지
- 비공개 기록 미노출
- 공유 게시글 삭제 또는 비공개 전환
- 모바일 피드 레이아웃

## 권장 구현 순서

1. 공개 범위/개인정보 정책 설계
2. 공유 가능한 독서 성장 카드 정의
3. 구조화된 공유 게시글 API
4. 공개 피드 조회
5. 좋아요/좋아요 취소
6. 공개 프로필
7. 신고/차단
8. 댓글 또는 팔로우 검토

## 남은 리스크

- 초기 사용자가 적으면 피드가 비어 보일 수 있다.
- moderation 체계 없이 자유 게시글을 열면 운영 리스크가 크다.
- 독서 기록 공개는 개인정보 민감도가 있을 수 있다.
- 좋아요 수가 독서 경험을 경쟁적으로 만들 수 있다.
- SNS가 추천 앱의 핵심 흐름을 흐릴 수 있으므로 후순위로 유지해야 한다.
