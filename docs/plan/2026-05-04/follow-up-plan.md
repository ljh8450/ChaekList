# 2026-05-04 후속 구현 계획

## 기준

- 작성일: 2026-05-04
- 최신화일: 2026-05-04
- 역할: planner
- 범위: README.md의 후속 계획 중 `비대면 모각독 기능`을 실제 구현 가능한 세부 계획으로 최신화한다.
- 주의: 이 문서는 계획 문서다. 코드, API, DB schema 변경은 포함하지 않는다.

## 확인한 현재 구조

- Frontend는 React Router 기반이며 `BookDetailPage`, `SocialFeedPage`, `PublicProfilePage`, `NotificationsPage`, `AdminModerationPage`가 분리되어 있다.
- Backend는 Spring Boot REST API이며 `book`, `mypage`, `social`, `search`, `auth` 도메인이 있다.
- Social 영역은 공개 피드, 게시글 생성, 좋아요, 신고, 차단, 관리자 숨김, 저장형 알림, 미디어 첨부를 이미 포함한다.
- 독서 성장 계산은 `MyPageService`에서 현재 데이터 기반으로 계산하며, SNS 활동 점수를 월간 보조 점수로 소폭 반영한다.
- 모각독은 책 상세, Social 공유, 알림, 독서 성장/배지와 연결되므로 frontend/backend/DB를 모두 건드리는 기능이다.

## 모각독 MVP 목표

- ChaekList를 책 추천에서 끝내지 않고, 사용자가 특정 시간에 실제로 읽도록 돕는 온라인 모각독 기능을 추가한다.
- 초기 버전은 대면 만남 없이 온라인 방 생성, 참여, 종료 후 인증만 지원한다.
- 채팅, 화상, 위치, 정산, 대면 스터디 모집은 MVP에서 제외한다.
- 참여 완료, 연속 참여, 정상적인 방 운영 이력을 기반으로 모각독 배지와 독서 성장 보조 점수를 제공한다.

## MVP 성공 기준

1. 사용자는 로그인 후 특정 책 기준으로 모각독 방을 만들 수 있다.
2. 사용자는 모집 중인 방에 참여하거나 참여 취소할 수 있다.
3. 사용자는 방 상세에서 책, 시간, 참여자 수, 상태, 설명을 확인할 수 있다.
4. 방 종료 후 참여자는 한 줄 인증 또는 읽은 분량을 기록할 수 있다.
5. 인증 완료를 기준으로 참여 완료, 모각독 배지, 독서 성장 보조 점수를 계산한다.
6. 모각독 관련 공개 공유는 기존 Social feed 정책을 재사용한다.
7. 신고, 차단, 관리자 숨김 정책은 기존 Social moderation 정책과 충돌하지 않는다.

## 범위

### 포함

- 모각독 방 생성
- 모각독 방 목록 조회
- 책별 진행 예정/진행 중 모각독 조회
- 모각독 방 상세 조회
- 모각독 참여
- 모각독 참여 취소
- 방 종료 후 인증 기록
- 내 모각독 참여 이력 조회
- 모각독 완료 기반 배지 계산
- 독서 성장에 모각독 보조 점수 반영
- 모각독 완료 또는 방 생성 결과의 Social 공유 흐름
- 시작 전/종료 후 저장형 알림 검토

### 제외

- 채팅
- 화상
- 위치 기반 대면 모집
- 참가비/정산
- 팔로우/DM
- 실시간 push
- 외부 캘린더 연동
- 자동 출석 체크

## 데이터 모델 계획

구현 전 DB schema 변경 승인이 필요하다.

### `reading_rooms`

- `id`
- `host_user_id`
- `book_id`
- `title`
- `description`
- `start_at`
- `end_at`
- `max_participants`
- `status`: `RECRUITING`, `IN_PROGRESS`, `ENDED`, `CANCELED`
- `visibility`: MVP는 `PUBLIC`만 허용
- `created_at`
- `updated_at`

정책:

- `start_at`은 `end_at`보다 이전이어야 한다.
- 최소 진행 시간은 20분으로 시작한다.
- 최대 진행 시간은 4시간으로 시작한다.
- `max_participants`는 2명 이상 30명 이하로 시작한다.
- 같은 사용자는 같은 시간대에 하나의 활성 방에만 참여할 수 있다.
- 방장은 자동 참여자로 등록한다.
- 방 생성 후 시작 전에는 방장이 취소할 수 있다.

### `reading_room_participants`

- `id`
- `room_id`
- `user_id`
- `status`: `JOINED`, `CANCELED`, `COMPLETED`
- `joined_at`
- `canceled_at`
- `completed_at`

정책:

- `(room_id, user_id)`는 중복될 수 없다.
- 모집 정원이 찬 방에는 참여할 수 없다.
- 시작 후 참여 취소는 MVP에서 제한한다.
- 참여 완료는 인증 생성 완료 시점에만 전환한다.

### `reading_room_checkins`

- `id`
- `room_id`
- `user_id`
- `note`
- `progress`
- `created_at`

정책:

- `note`와 `progress` 중 하나 이상은 필요하다.
- 한 사용자는 한 방에 인증을 1회만 남긴다.
- 인증은 `end_at` 이후부터 허용한다.
- 너무 긴 인증 문구를 막기 위해 `note`는 300자 이하로 시작한다.

### `reading_room_reports` 또는 Social 신고 재사용

권장:

- MVP에서는 새 신고 테이블을 만들기보다 기존 `social_reports`의 target type 확장을 검토한다.
- 다만 기존 구현이 Social 게시글/닉네임 중심이면 `READING_ROOM` target type 추가가 가장 작은 변경인지 먼저 확인한다.

## API 계획

신규 backend 후보: `backend/src/main/java/com/example/chaeklist/domain/readingroom`

### 공개/선택 인증 API

- `GET /api/reading-rooms?status={status}&bookId={bookId}&limit={limit}`
  - 공개 모각독 목록 조회
  - 기본은 모집 중/진행 전 방 최신 시작순
- `GET /api/books/{bookId}/reading-rooms?limit={limit}`
  - 책 상세 화면에서 노출할 해당 책 기준 모각독 조회
- `GET /api/reading-rooms/{roomId}`
  - 모각독 방 상세 조회

### 로그인 필요 API

- `POST /api/reading-rooms`
  - 방 생성
  - 요청: `bookId`, `title`, `description`, `startAt`, `endAt`, `maxParticipants`, `idempotencyKey`
  - 응답: 방 상세
- `POST /api/reading-rooms/{roomId}/participants`
  - 참여
  - 중복 참여 요청은 멱등 처리
- `DELETE /api/reading-rooms/{roomId}/participants/me`
  - 내 참여 취소
- `POST /api/reading-rooms/{roomId}/checkins`
  - 종료 후 인증
  - 인증 성공 시 참여 상태를 `COMPLETED`로 전환
- `GET /api/me/reading-rooms?status={status}&limit={limit}`
  - 내가 만든/참여한 모각독 조회

### 관리자/운영 API 후보

- `POST /api/reading-rooms/{roomId}/reports`
  - 모각독 방 신고
- `POST /api/admin/reading-rooms/{roomId}/hide`
  - 관리자 숨김 또는 취소
- `DELETE /api/admin/reading-rooms/{roomId}/hide`
  - 관리자 숨김 해제

MVP에서는 기존 관리자 moderation 화면에 바로 붙이기보다 신고 접수와 공개 목록 제외 정책을 먼저 구현한다.

## Backend 구현 순서

1. `readingroom` 도메인 뼈대 추가 -> verify: controller/service/dto가 기존 Social API 스타일과 일치하는지 확인
2. DB schema 추가 -> verify: H2 테스트에서 schema 초기화와 기본 CRUD가 통과
3. 방 생성 API 구현 -> verify: 유효성, 방장 자동 참여, idempotency 테스트
4. 목록/책별/상세 조회 API 구현 -> verify: 공개 상태, 정원, 참여자 수, 책 요약 응답 테스트
5. 참여/취소 API 구현 -> verify: 정원 초과, 중복 참여, 시간대 중복 참여, 시작 후 취소 제한 테스트
6. 종료 후 인증 API 구현 -> verify: 종료 전 인증 거부, 중복 인증 거부, 인증 후 `COMPLETED` 전환 테스트
7. 내 모각독 조회 API 구현 -> verify: 만든 방/참여 방이 최신순으로 반환되는지 테스트
8. 독서 성장 보조 점수와 배지 계산 연동 -> verify: 모각독 점수 상한과 배지 조건 테스트
9. Social 공유 연동 -> verify: 모각독 완료 공유 게시글이 기존 feed 정책을 따르는지 테스트
10. 신고/숨김 정책 연결 -> verify: 신고 또는 관리자 숨김 방이 공개 목록에서 제외되는지 테스트

## Frontend 화면 계획

### 신규 화면 후보

- `frontend/src/pages/ReadingRoomsPage.jsx`
  - 전체 모각독 목록
  - 상태 필터: 모집 중, 진행 예정, 종료
  - 책 제목, 시간, 참여자 수, 상태 표시
- `frontend/src/pages/ReadingRoomDetailPage.jsx`
  - 방 상세
  - 참여/취소 버튼
  - 종료 후 인증 입력
  - 참여자 수와 내 참여 상태 표시
- `frontend/src/pages/MyReadingRoomsPage.jsx`
  - 내가 만든/참여한 모각독 목록
  - 인증 필요 방 강조

### 기존 화면 변경 후보

- `BookDetailPage.jsx`
  - 해당 책의 모집 중/진행 예정 모각독 섹션 추가
  - `이 책으로 함께 읽기 열기` 버튼 추가
- `Header.jsx`
  - MVP에서는 메뉴 추가 여부를 보수적으로 결정한다.
  - 독립 탐색이 필요하면 `모각독` 링크를 추가한다.
- `MyPage.jsx`
  - 모각독 배지 또는 최근 완료 이력 요약 추가를 검토한다.
- `SocialFeedPage.jsx` / `SocialPostCard.jsx`
  - 모각독 완료 공유 타입을 기존 카드 패턴에 맞춰 추가한다.

## Frontend 구현 순서

1. 라우트와 목록 화면 추가 -> verify: `/reading-rooms` 접근과 비로그인 공개 조회 확인
2. 방 생성 UI 추가 -> verify: 로그인 사용자만 생성 가능하고 필수값 검증 메시지 확인
3. 책 상세 모각독 섹션 추가 -> verify: `/books/:bookId`에서 해당 책 방 목록과 생성 진입 확인
4. 방 상세 화면 추가 -> verify: 공개 상세, 참여 가능 상태, 정원 표시 확인
5. 참여/취소 UI 연결 -> verify: 버튼 클릭 후 참여자 수와 내 상태 갱신 확인
6. 종료 후 인증 UI 연결 -> verify: 종료 전 비활성, 종료 후 인증 가능 상태 확인
7. 내 모각독 화면 추가 -> verify: 참여/완료/인증 필요 상태 확인
8. 모각독 공유 카드 추가 -> verify: 공유 후 공개 피드 카드 렌더링 확인

## 배지와 독서 성장 정책

모각독은 독서 성장의 핵심 점수가 아니라 보조 점수로만 반영한다.

### 배지 후보

- `FIRST_READING_ROOM`: 모각독 첫 참여 완료
- `READING_ROOM_STREAK`: 3회 연속 또는 3주 연속 참여 완료
- `ROOM_HOST`: 직접 만든 방에서 참여자 2명 이상이 완료
- `STEADY_READER`: 한 달 내 모각독 완료 4회

### 점수 후보

- 인증 완료한 모각독 1회당 2점
- 월간 최대 6점
- 방 생성만으로는 점수를 주지 않는다.
- 참여 신청만으로는 점수를 주지 않는다.
- 관리자 숨김 또는 취소된 방의 인증은 점수에서 제외한다.

## 어뷰징 방지 정책

- 하루 방 생성 횟수는 사용자당 3회로 시작한다.
- 같은 시간대 중복 참여를 제한한다.
- 최소 진행 시간은 20분으로 시작한다.
- 최대 진행 시간은 4시간으로 시작한다.
- 같은 방 인증은 사용자당 1회만 허용한다.
- 반복 신고 또는 관리자 숨김 이력이 많은 사용자의 방 생성 제한은 운영 도구 확장 단계에서 검토한다.
- 인증 텍스트는 300자 이하로 제한한다.
- 참여 완료 기준은 `checkin` 생성 완료로 고정한다.

## 공개/개인정보 정책

- 모각독 방은 MVP에서 공개 방만 지원한다.
- 참여자 목록은 닉네임과 공개 허용 배지만 표시할지, 참여자 수만 표시할지 구현 전 결정한다.
- 이메일, 비공개 독서 기록, 비공개 관심 분야는 어떤 모각독 API에도 노출하지 않는다.
- 차단한 사용자의 방을 목록에서 숨길지, 참여만 제한할지 구현 전 결정한다.
- 관리자 숨김 또는 취소된 방은 공개 목록, 책 상세, 검색 후보에서 제외한다.

## 알림 연동 계획

현재 저장형 알림 API가 있으므로 실시간 push 없이 아래만 검토한다.

- 모각독 시작 전 알림
- 모각독 종료 후 인증 요청 알림
- 방장에게 새 참여자 알림

주의:

- 스케줄러가 필요하면 별도 구현 범위로 분리한다.
- MVP에서는 사용자가 화면에 진입했을 때 인증 필요 상태를 보여주는 방식으로 먼저 시작할 수 있다.

## 구현 전 결정 필요 사항

- DB schema 변경 승인
- 모각독 방 참여자 목록 공개 수준
- 차단 사용자와 모각독 방 노출/참여 정책
- 방 상태 자동 전환을 조회 시 계산으로 처리할지, 스케줄러/배치로 저장할지
- Social 공유 타입을 새 타입으로 추가할지, 기존 `TEXT`/구조화 카드에 포함할지
- 관리자 숨김을 별도 `reading_room_admin_hidden`으로 둘지, 기존 Social moderation target 확장으로 처리할지

## 권장 구현 순서

1. 모각독 DB/API 최소 흐름
2. 책 상세의 모각독 섹션과 방 생성/상세/참여 UI
3. 종료 후 인증과 내 모각독 화면
4. 독서 성장 보조 점수와 모각독 배지
5. Social 공유 카드
6. 신고/숨김 정책과 관리자 운영 연결
7. 저장형 알림 연동

## 기존 후속 항목 정리

아래 항목은 모각독 MVP 이후 별도 우선순위로 유지한다.

1. 공개 프로필 콘텐츠 확장
2. 공개 유저 검색 탐색성 추가 보강
3. 공개 피드 품질 개선
4. SNS 활동 점수 보정
5. 미디어 첨부 확장
6. 알림 기능 확장
7. moderation 운영 도구
8. 외부 책 검색 및 import

## 검증 계획

문서만 변경한 경우:

- 별도 빌드 검증은 필요하지 않다.
- 링크와 내용 범위만 확인한다.

후속 frontend 변경 시:

```powershell
cd frontend
npm run build
```

후속 backend 변경 시:

```powershell
cd backend
.\gradlew.bat test
```

후속 frontend/backend 동시 변경 시:

```powershell
cd frontend
npm run build
```

```powershell
cd backend
.\gradlew.bat test
```
