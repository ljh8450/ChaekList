# 2026-05-04 후속 구현 계획

## 기준

- 작성일: 2026-05-04
- 최신화일: 2026-05-05
- 역할: planner
- 기준 문서:
  - `README.md`
  - `docs/plan/2026-05-04/mvp-gap-and-expansion-check-plan.md`
- 범위: README.md의 `비대면 모각독 기능`을 현재 구현 상태와 남은 후속 구현 계획 기준으로 최신화한다.
- 주의: 이 문서는 계획 문서이며, 코드/API/DB schema 변경 자체를 수행하지 않는다.

## 현재 구현 상태 요약

### Backend

- `domain/readingroom` 기반 모각독 API가 구현되어 있다.
- 공개/인증 조회 API가 구현되어 있다.
  - `GET /api/reading-rooms`
  - `GET /api/books/{bookId}/reading-rooms`
  - `GET /api/reading-rooms/{roomId}`
  - `GET /api/me/reading-rooms`
- 생성/참여/취소/인증 API가 구현되어 있다.
  - `POST /api/reading-rooms`
  - `POST /api/reading-rooms/{roomId}/participants`
  - `DELETE /api/reading-rooms/{roomId}/participants/me`
  - `POST /api/reading-rooms/{roomId}/checkins`
- 신고/관리자 숨김 API가 구현되어 있다.
  - `POST /api/reading-rooms/{roomId}/reports`
  - `POST /api/admin/reading-rooms/{roomId}/hide`
  - `DELETE /api/admin/reading-rooms/{roomId}/hide`
- `READING_ROOM` 공유 타입이 Social feed 카드와 신고 대상에 연동되어 있다.
- 모각독 참여 완료 기반 독서 성장 보조 점수와 기본 배지가 연동되어 있다.
- 저장형 알림 기반으로 모각독 시작 전 알림, 종료 후 인증 요청 알림, 새 참여자 방장 알림이 구현되어 있다.
- `docs/mysql-ddl.sql`에는 모각독 관련 테이블이 반영되어 있다.

### Frontend

- 모각독 라우팅과 Header 진입점이 구현되어 있다.
  - `/reading-rooms`
  - `/reading-rooms/:roomId`
  - `/me/reading-rooms`
- 전체 모각독 목록, 방 생성, 책 검색 기반 방 생성/필터, 방 상세, 참여/취소, 종료 후 인증 입력 화면이 구현되어 있다.
- 책 상세 화면에서 해당 책 기준 모각독 목록과 방 생성 진입이 구현되어 있다.
- 내 모각독 화면에서 참여/완료/인증 필요 상태 확인이 구현되어 있다.
- Social feed 카드에서 `READING_ROOM` 타입 표시가 구현되어 있다.

## 모각독 MVP 목표

- ChaekList가 책 추천에서 끝나지 않고, 사용자가 특정 시간에 실제로 책을 읽도록 돕는 온라인 모각독 기능을 제공한다.
- 초기 버전은 대면 만남 없이 온라인 방 생성, 참여, 취소, 상세 조회, 종료 후 인증을 지원한다.
- 채팅, 영상, 위치, 정산, 대면 스터디 모집, 실시간 push는 MVP에서 제외한다.
- 참여 완료와 인증 완료를 기준으로 배지와 독서 성장 보조 점수를 계산한다.

## MVP 성공 기준

1. 사용자는 로그인 후 책 검색으로 책을 선택해 모각독 방을 만들 수 있다. -> verify: 방 생성 후 상세 화면 이동 확인
2. 사용자는 모집 중인 방에 참여하거나 시작 전 참여 취소할 수 있다. -> verify: 상세 화면의 참여 상태와 버튼 상태 갱신 확인
3. 사용자는 방 상세에서 책, 시간, 참여 인원, 상태, 설명을 확인할 수 있다. -> verify: 공개 상세 조회와 로그인 사용자 상태 조회 확인
4. 종료 후 참여자는 한 번만 인증 메모 또는 읽은 분량을 기록할 수 있다. -> verify: 인증 성공 후 중복 인증 거부 확인
5. 인증 완료는 내 모각독, 독서 성장, 배지 계산에 반영된다. -> verify: 마이페이지 성장 응답과 배지 응답 확인
6. 모각독 공유 게시글은 기존 Social feed 정책을 따른다. -> verify: 공개/비공개, 삭제, 신고, 숨김 정책 확인
7. 알림 설정이 꺼진 사용자는 모각독 저장형 알림을 받지 않는다. -> verify: 알림 설정별 생성 여부 확인

## 데이터 모델 최신 상태

### `reading_rooms`

- 방 기본 정보와 상태를 저장한다.
- 주요 필드: `host_user_id`, `book_id`, `title`, `description`, `start_at`, `end_at`, `max_participants`, `status`, `visibility`, `created_at`, `updated_at`
- 정책:
  - `start_at`은 `end_at`보다 이전이어야 한다.
  - MVP visibility는 `PUBLIC` 중심으로 운영한다.
  - 방장은 방 생성 시 자동 참여자로 등록한다.
  - 시작 전까지만 취소할 수 있다.

### `reading_room_participants`

- 방 참여자와 참여 상태를 저장한다.
- 주요 상태: `JOINED`, `CANCELED`, `COMPLETED`
- 정책:
  - 같은 방에 같은 사용자는 중복 참여할 수 없다.
  - 정원 초과 참여를 막는다.
  - 인증 성공 시 `COMPLETED`로 전환한다.

### `reading_room_checkins`

- 종료 후 인증 기록을 저장한다.
- `note` 또는 `progress` 중 하나 이상이 필요하다.
- 같은 사용자는 같은 방에 인증을 한 번만 남길 수 있다.
- 인증은 방 종료 후에만 허용한다.

### `reading_room_admin_hidden`

- 관리자 숨김 처리된 모각독 방을 저장한다.
- 공개 목록, 책 상세 목록, 검색 후보에서 숨김 방을 제외하는 기준으로 사용한다.

### `reading_room_notification_events`

- 스케줄러 기반 저장형 알림의 중복 생성을 막기 위한 이벤트 이력을 저장한다.
- 모각독 시작 전 알림과 종료 후 인증 요청 알림의 dedupe 기준으로 사용한다.

## Backend 후속 구현 계획

1. 모각독 핵심 API 안정화 -> verify: `cd backend && .\gradlew.bat test`
2. 시간/상태 경계 케이스 보강 -> verify: 시작 직전 참여, 시작 후 취소, 종료 전 인증, 종료 후 중복 인증 테스트
3. 알림 스케줄러 검증 보강 -> verify: 알림 설정 on/off, 중복 이벤트, 시간 창 경계 테스트
4. Social 공유/신고/숨김 정책 회귀 테스트 추가 -> verify: `READING_ROOM` feed 표시, 신고, 관리자 숨김 후 목록 제외 테스트
5. 독서 성장/배지 조건 문서와 테스트 정렬 -> verify: 첫 완료, 연속 완료, 월간 보조 점수 상한 테스트

## Frontend 후속 구현 계획

1. 책 검색 기반 생성/필터 UX 안정화 -> verify: `/reading-rooms`에서 책 제목 검색, 선택, 초기화, 목록 갱신 확인
2. 방 생성 폼 검증 메시지 정리 -> verify: 필수값 누락, 시간 역전, 정원 범위 오류 확인
3. 상세 화면 액션 상태 회귀 확인 -> verify: 비로그인, 로그인 미참여, 참여자, 완료자 상태별 버튼 확인
4. 내 모각독 화면 상태 표시 개선 -> verify: 모집 중/진행 예정/인증 필요/완료가 구분되는지 확인
5. 책 상세 연동 회귀 확인 -> verify: `/books/:bookId`에서 해당 책 방 목록과 생성 진입 확인
6. Social 공유 카드 표시 확인 -> verify: 모각독 공유 게시글이 feed에서 깨지지 않는지 확인

## 알림 연동 계획

현재 저장형 알림 API가 있으므로 실시간 push 없이 아래 범위까지 구현된 상태로 본다.

- 구현됨:
  - 모각독 시작 전 알림
  - 모각독 종료 후 인증 요청 알림
  - 새 참여자 발생 시 방장 알림
  - 알림 설정을 끈 사용자 제외
- 후속:
  - 프론트 알림 목록에서 모각독 알림 문구와 이동 경로 확인
  - 스케줄러 시간 창 경계 테스트 보강
  - 실시간 push 필요성은 MVP 이후 재평가

## 개인정보와 운영 정책

- MVP 모각독 방은 공개 방 중심으로 운영한다.
- 비공개 독서 기록, 비공개 관심 분야, 비공개 배지는 모각독 API 응답에 노출하지 않는다.
- 참여자 목록 전체 공개 여부는 후속 결정 항목으로 둔다.
- 차단 사용자의 방 노출/참여 제한 정책은 Social 차단 정책과 맞춘다.
- 관리자 숨김 방은 공개 목록, 책 상세 목록, 검색/추천 후보에서 제외한다.

## 남은 결정 사항

- 참여자 목록을 전체 공개할지, 참여자에게만 공개할지 결정한다.
- 차단 사용자 간 모각독 방 노출과 참여 제한 범위를 확정한다.
- 방 상태 전환을 조회 시 계산으로 유지할지, 스케줄러/배치로 저장할지 결정한다.
- 모각독 알림 클릭 시 이동 경로를 방 상세로 고정할지, 내 모각독으로 보낼지 결정한다.
- 연속 참여 배지 기준을 일 단위로 볼지 주 단위로 볼지 확정한다.

## 권장 구현 순서

1. Backend 회귀 테스트 보강
2. Frontend 책 검색/생성/필터 UX 회귀 확인
3. 상세/내 모각독 상태별 수동 QA
4. 알림 문구와 이동 경로 확인
5. 배지/성장 점수 조건 문서와 테스트 정렬
6. MVP 이후 공개 프로필, feed 정렬, 미디어 첨부, 외부 책 검색 순으로 후속 검토

## 기존 후속 항목 정리

모각독 MVP 이후 아래 항목은 별도 우선순위로 유지한다.

1. 공개 프로필 콘텐츠 확장
2. 공개 유저 검색 결과 summary 개선
3. 공개 feed 정렬/필터 개선
4. SNS 활동 점수 정책 보정
5. 미디어 첨부 확장
6. 알림 기능 확장
7. moderation 운영 도구
8. 외부 책 검색과 import

## 검증 계획

문서만 변경한 경우:

- 별도 빌드 검증은 필요하지 않다.
- Markdown 내용 범위와 링크/명령 문구만 확인한다.

후속 frontend 변경:

```powershell
cd frontend
npm run build
```

후속 backend 변경:

```powershell
cd backend
.\gradlew.bat test
```
