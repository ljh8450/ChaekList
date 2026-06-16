# ChaekList

ChaekList는 사용자가 읽을 만한 교양서를 빠르게 발견하고, 읽기 활동을 기록하며, 선택적으로 공개 피드에 독서 활동을 공유할 수 있는 full-stack 웹 애플리케이션입니다.

## PT 자료

[ChaekList-Presentation.pdf](https://github.com/user-attachments/files/29023770/ChaekList-Presentation.pdf)

## 서비스 개요

ChaekList는 일반 베스트셀러 목록보다 “지금 읽기 좋은 교양서” 탐색에 초점을 둡니다. 비로그인 사용자는 공개 홈, 랭킹, 책 상세, 검색을 이용할 수 있고, 로그인 사용자는 관심 분야, 독서 목적, 읽은 책, 저장한 책을 바탕으로 개인화 추천과 독서 성장 상태를 확인할 수 있습니다.

공개 SNS 기능은 opt-in 공유를 기본으로 합니다. 사용자가 직접 공개한 게시글만 공개 피드와 검색에 노출되며, 비공개 독서 기록은 노출하지 않습니다.

## 현재 구현 상태

### 책 탐색과 추천

- 공개 홈 추천, 인기 책, 급상승 책, 카테고리별 랭킹을 제공합니다.
- 책 상세에서 요약, 추천 이유, 교양 필터 리포트, 추천 근거, 읽기 가이드, 비슷한 책을 제공합니다.
- 내부 DB 기반 책 검색은 제목/저자 기준으로 동작합니다.
- 책 상호작용을 기록합니다.
  - `READ`
  - `SAVE`
  - `UNSAVE`
  - `DISMISS`
- 개인화 홈은 관심 분야, 읽은 책, 저장한 책, 독서 목적을 반영합니다.
- 추천 히스토리는 중복 저장을 방지합니다.

### 온보딩과 마이페이지

- 온보딩에서 관심 분야, 읽은 책, 독서 목적을 선택할 수 있습니다.
- 독서 목적은 1~3개 선택할 수 있습니다.
- 마이페이지에서 관심 분야, 독서 목적, 읽은 책, 저장한 책, 추천 히스토리를 확인할 수 있습니다.
- 저장한 책을 읽은 책으로 전환하거나 저장 취소할 수 있습니다.
- 관심 없는 책은 추천 후보에서 제외할 수 있습니다.

### 독서 성장

- 마이페이지 응답에 `readingGrowth`가 포함됩니다.
- 월간 읽은 책 수, 저장 후 읽음 전환 수, 카테고리 다양성, 추천 전환 수를 계산합니다.
- 대표 배지와 기본 배지를 계산합니다.
- Header에는 로그인 사용자의 대표 배지를 표시합니다.
- 독서 성장 레벨에는 SNS 활동을 보조 점수로 소폭 반영합니다.
  - 활성 게시글 최대 5개까지 개당 2점
  - 받은 좋아요 최대 5개까지 개당 1점
  - 삭제/관리자 숨김 게시글은 제외

### 통합 검색

- Header 검색 입력에서 검색 결과 페이지로 이동합니다.
- 통합 검색은 책, 키워드, 공개 게시글, 공개 유저를 대상으로 합니다.
- 검색어는 최소 2글자 이상이어야 합니다.
- 공개 게시글 검색은 공개/활성 게시글만 대상으로 합니다.
- 공개 유저 검색은 아래 공개 정보만 대상으로 합니다.
  - 닉네임
  - 공개 게시글 내용
  - 공개 게시글과 연결된 책 제목/저자
  - 공개 게시글 타입
  - 공개 관심 분야
- 대표 배지명은 공개 유저 검색 조건에 포함하지 않습니다.
- 비공개 게시글, 비공개 관심 분야, 비공개 프로필은 검색에 노출하지 않습니다.

### SNS 1차 기능

- 공개 피드를 제공합니다.
- 공유 게시글을 생성할 수 있습니다.
- 자유 텍스트 게시글을 지원합니다.
- 책 공유, 성장 카드 공유, 배지 공유 유형을 지원합니다.
- 본인 게시글은 공개/비공개 전환할 수 있습니다.
- 본인 게시글은 삭제할 수 있습니다.
- 좋아요와 좋아요 취소를 지원합니다.
- 좋아요는 게시글당 사용자 1회로 멱등성을 보장합니다.
- 공개 프로필 기본 조회를 제공합니다.
- 공개 게시글이 있는 활성 사용자는 공개 프로필 조회 대상이 됩니다.
- 대표 배지는 공개 설정이 허용된 경우에만 공개 화면에 표시됩니다.
  - 공개 피드 사용자명 앞
  - 공개 프로필 사용자명 앞
  - 공개 유저 검색 사용자명 앞

### 설정과 개인정보

- 설정 페이지에서 공개 설정, 알림 설정, 회원 탈퇴 진입을 제공합니다.
- 설정 페이지에서 내가 작성한 글과 좋아요 누른 글을 확인할 수 있습니다.
- 비활성화 계정의 공개 게시글은 feed/search/profile에서 숨깁니다.
- 탈퇴 시 기존 공개 게시글은 삭제하지 않고 작성자 정보를 익명화합니다.
- 배지 공개 여부에 따라 대표 배지 공개 노출을 제어합니다.

### moderation 최소 기능

- 게시글 신고를 지원합니다.
- 사용자 닉네임 신고를 지원합니다.
- 사용자 차단을 지원합니다.
- 관리자 숨김 처리된 게시글은 feed/search/profile에서 제외합니다.
- 반복 게시 횟수 제한은 두지 않되, 생성 요청의 멱등성을 보장합니다.

## 후속 계획

상세 계획은 `docs/plan/2026-04-30/follow-up-plan.md`를 기준으로 합니다.

### 1. 공개 프로필 콘텐츠 확장

- 공개 프로필에 해당 유저의 공개 게시글 목록을 추가합니다.
- 공개 프로필에 좋아요가 많은 공개 게시글과 공개 독서 통계 요약을 추가하는 방안을 검토합니다.
- 공개 프로필 내 게시글 타입 필터를 제공합니다.
- 비공개 독서 기록, 비공개 관심 분야, 비공개 배지는 노출하지 않습니다.

### 2. 공개 유저 검색 결과 개선

- 검색 결과 summary를 개선합니다.
  - 공개 게시글 수
  - 최근 공개 활동
  - 공개 관심 분야 일부
- 닉네임 직접 매칭, 최근 공개 활동, 공개 게시글 수를 고려한 정렬 보정을 검토합니다.
- 검색 조건은 공개 데이터로만 제한합니다.

### 3. 공개 피드 품질 개선

- 최신순 외 정렬 옵션을 검토합니다.
  - 좋아요 많은 순
  - 내 관심 분야와 가까운 순
- 피드 타입 필터를 고도화합니다.
- 신고 또는 차단 후 클라이언트에서 즉시 숨김 처리하는 UX를 개선합니다.

### 4. SNS 활동 점수 정책 보정

- SNS 활동 점수를 월간 기준으로 볼지 누적 기준으로 볼지 결정합니다.
- 본인 좋아요를 받은 좋아요 점수에서 제외할지 결정합니다.
- 레벨 진행률 설명에 SNS 활동 반영 여부를 표시할지 검토합니다.
- 독서 성장의 핵심 점수는 계속 독서 활동 중심으로 유지합니다.

### 5. 미디어 첨부

- 이미지 첨부부터 검토합니다.
- 파일 크기, 개수, MIME type, 저장소, 썸네일 정책을 먼저 정합니다.
- 영상 첨부는 별도 단계로 분리합니다.
- 신고/관리자 숨김 정책이 미디어에도 적용되도록 합니다.

### 6. 알림 확장

- 서버 저장형 알림 목록을 먼저 구현합니다.
- 좋아요 알림, 신고 처리 상태 알림, 서비스 공지 알림을 검토합니다.
- 실시간 push는 후순위입니다.

### 7. moderation 운영 도구

- 관리자 신고 목록과 신고 상세를 제공합니다.
- 게시글 숨김/해제, 닉네임 신고 처리, 운영자 메모를 검토합니다.
- 자동 moderation은 후순위이며, 사용자 신고 기반 수동 처리를 우선합니다.

### 8. 외부 책 검색 및 import

- 카카오 도서 검색 연동을 검토합니다.
- 내부 DB 결과와 외부 검색 결과를 병합할지 결정합니다.
- ISBN13 기준 중복 확인 정책을 정합니다.
- 외부 책 선택 시 내부 `books` 저장 후 기존 `READ`/`SAVE` interaction 흐름을 재사용합니다.
- API key/env 변경이 필요하므로 구현 전 별도 승인이 필요합니다.

### 9. 비대면 모각독 기능

- ChaekList를 책 추천에서 끝내지 않고, 사용자가 실제로 읽도록 돕는 비대면 모각독 기반 독서 성장 서비스로 확장합니다.
- 초기 버전은 대면 만남 없이 온라인 모각독만 지원합니다.
- 사용자는 특정 책을 기준으로 모각독 방을 생성하고 참여할 수 있습니다.
- 모각독 방은 제목, 책, 시작/종료 시간, 최대 인원, 설명, 모집 중/진행 중/종료 상태를 가집니다.
- 책 상세 페이지에서 해당 책으로 진행 중인 모각독을 확인하거나 `이 책으로 함께 읽기 열기` 흐름으로 새 모각독을 만들 수 있도록 검토합니다.
- 모각독은 정해진 시간에 각자 책을 읽고, 종료 후 한 줄 인증 또는 읽은 분량을 기록하는 방식으로 운영합니다.
- 1차 MVP에서는 채팅, 화상, 대면, 정산, 위치 기능을 제외하고 방 생성, 참여/취소, 상세 조회, 종료 후 인증, 피드 공유, 기본 배지만 우선 검토합니다.
- 모각독 참여 완료, 연속 참여, 정상적인 방 운영 이력을 기반으로 별도 배지를 제공합니다.
- 모각독 활동 점수는 독서 성장의 보조 점수로만 반영하며, 월간 상한을 두어 독서 기록 중심의 성장 구조를 유지합니다.
- 참여 신청이 아니라 종료 후 인증 완료를 기준으로 참여 완료, 배지, 성장 보조 점수를 계산합니다.
- 같은 시간대 중복 참여 제한, 최소 진행 시간, 하루 방 생성 횟수 제한, 모집글 신고 누적 시 생성 제한 등 어뷰징 방지 정책을 검토합니다.
- 신고, 차단, 관리자 숨김 정책은 기존 Social moderation 정책을 재사용합니다.
- 알림 확장 단계에서 모각독 시작 전 알림, 종료 알림, 새 참여자 알림을 함께 검토합니다.
- 대면 스터디 모집은 안전, 어뷰징, 개인정보, 운영 리스크 검토 후 후순위로 분리합니다.

## 주요 API

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

### My Page

- `GET /api/me/mypage`
- `GET /api/me/onboarding-status`
- `GET /api/me/onboarding-options`
- `PUT /api/me/onboarding`
- `POST /api/me/books/{bookId}/interactions`
- `GET /api/me/reading-growth/primary-badge`

### Search

- `GET /api/search?query={query}&type={all|books|keywords|posts|users}&limit={limit}`
- `GET /api/search/books?query={query}&limit={limit}`
- `GET /api/search/posts?query={query}&limit={limit}`
- `GET /api/search/users?query={query}&limit={limit}`

### Social

- `GET /api/social/feed`
- `POST /api/social/posts`
- `GET /api/me/social/posts`
- `GET /api/me/social/liked-posts`
- `PATCH /api/social/posts/{postId}`
- `DELETE /api/social/posts/{postId}`
- `POST /api/social/posts/{postId}/likes`
- `DELETE /api/social/posts/{postId}/likes`
- `POST /api/social/posts/{postId}/reports`
- `GET /api/users/{userId}/public-profile`
- `POST /api/users/{userId}/reports`
- `POST /api/users/{userId}/blocks`
- `DELETE /api/users/{userId}/blocks`

### Settings

- `GET /api/me/settings`
- `PATCH /api/me/privacy-settings`
- `PATCH /api/me/notification-settings`
- `POST /api/me/withdraw`

### Auth

- 회원가입
- 로그인
- 토큰 기반 인증

## 화면 구조

### Home

- 오늘의 추천
- 인기 책
- 급상승 책
- 카테고리별 랭킹
- Header 통합 검색

### Ranking

- 전체 랭킹
- 카테고리별 랭킹
- 기간 필터

### Book Detail

- 책 기본 정보
- 교양 필터 리포트
- 추천 근거
- 읽기 가이드
- 비슷한 책
- 저장/읽음/관심 없음 액션

### Onboarding

- 관심 분야 선택
- 독서 목적 선택
- 읽은 책 검색 및 선택

### My Page

- 프로필 요약
- 독서 성장 카드
- 대표 배지
- 관심 분야
- 독서 목적
- 읽은 책
- 저장한 책
- 추천 히스토리

### Social

- 공개 피드
- 자유 텍스트 공유
- 책/성장 카드 공유 게시글
- 좋아요/좋아요 취소
- 신고/차단
- 공개 프로필

### Search

- 책 검색 결과
- 키워드 검색 결과
- 공개 게시글 검색 결과
- 공개 유저 검색 결과

### Settings

- 공개 설정
- 알림 설정
- 내가 작성한 글
- 좋아요 누른 글
- 회원 탈퇴 진입

## 배포 아키텍처 구조

아래 그림은 현재 구현된 기능과 `docs/plan/2026-04-30/follow-up-plan.md`의 확장 방향을 기준으로 실제 배포 시 나눌 수 있는 구조입니다.

### Frontend

```text
+----------------------------------------------------------------+
| Browser                                                        |
+----------------------------------------------------------------+
| React SPA                                                      |
|                                                                |
| Layer              Responsibilities                            |
| -----------------  ------------------------------------------- |
| App / Router       public, protected, social routes            |
| Auth State         access token, current user, primary badge   |
| Pages              Home, Ranking, Book Detail                  |
|                    Onboarding, My Page, Settings               |
|                    Search Results, Social Feed, Public Profile |
| Components         Header search, Book cards, Search panels    |
|                    SocialPostCard, Settings/Profile sections   |
| API Client         /api/** HTTPS REST JSON                     |
+----------------------------------------------------------------+
        |
        v
+----------------------------------------------------------------+
| Backend API                                                    |
+----------------------------------------------------------------+
```

Frontend 배포 단위:

- 정적 React build 산출물을 CDN 또는 web server에서 제공한다.
- API 요청은 `/api/**` 경로로 backend에 전달한다.
- 인증은 access token 기반이며, 보호된 화면에서 Authorization header를 포함한다.
- 공개 화면은 비로그인 접근을 허용하되, 비공개 데이터는 API에서 내려주지 않는다.

후속 확장 시 frontend에 추가될 영역:

- 공개 프로필 내 공개 게시글 목록
- 공개 유저 검색 summary 개선
- 피드 정렬/필터 UI
- 알림 목록 화면
- 관리자 moderation 화면
- 이미지 첨부 UI

### Backend

```text
+----------------------------------------------------------------+
| Spring Boot API Server                                         |
+----------------------------------------------------------------+
| Module             Responsibilities                            |
| -----------------  ------------------------------------------- |
| Auth               signup/login, token validation, bearer auth |
| Book/Recommend     home, rankings, trends, detail, search      |
|                    personalized recommendation                 |
| My Page/Growth     onboarding, interactions, growth, badges    |
| Search             books, keywords, public posts/users         |
| Social             posts, feed, likes, public profile          |
| Settings           privacy, notification, withdrawal           |
| Moderation         reports, blocks, hidden posts               |
+----------------------------------------------------------------+
        |
        | JDBC / JPA
        v
+----------------------------------------------------------------+
| MySQL                                                          |
+----------------------------------------------------------------+
| users, books, categories, keywords, rankings                   |
| recommendations, interactions, reading purposes                |
| social posts, likes, reports, blocks, hidden posts             |
| privacy settings, notification settings, public profiles       |
+----------------------------------------------------------------+
```

Backend 배포 단위:

- Spring Boot API server는 REST JSON API를 제공한다.
- MySQL을 영속 저장소로 사용한다.
- H2는 테스트 용도로 사용한다.
- 공개 feed/search/profile API는 visibility, status, block, hidden, withdrawal 정책을 서버에서 강제한다.
- 좋아요와 추천 저장 등 중복 가능 요청은 서비스 레벨에서도 멱등성을 보장한다.

후속 확장 시 backend에 추가될 영역:

- 공개 프로필 게시글 목록 API
- 공개 유저 검색 정렬 점수와 summary 보강
- 피드 정렬/필터 API
- 서버 저장형 알림 API
- 관리자 moderation API
- 이미지 첨부 저장소 연동
- 카카오 도서 검색/import 연동

### 배포 흐름

```text
+------------------+      +------------------+      +------------------+
| User             | ---> | CDN / Web Server | ---> | Browser SPA      |
+------------------+      +------------------+      +--------+---------+
                                                               |
                                                               | /api/** HTTPS
                                                               v
                                                      +--------+---------+
                                                      | Spring Boot API  |
                                                      +--------+---------+
                                                               |
                                  +----------------------------+----------------------------+
                                  |                                                         |
                                  v                                                         v
                         +--------+---------+                                      +--------+---------+
                         | MySQL            |                                      | External APIs    |
                         | primary storage  |                                      | Kakao Search     |
                         +------------------+                                      +------------------+
```

운영 시 기본 원칙:

- 공개 데이터는 opt-in을 기본으로 한다.
- 비공개 데이터 노출 방지는 frontend가 아니라 backend 정책으로 보장한다.
- 검색, 피드, 공개 프로필은 동일한 공개/숨김/탈퇴 정책을 공유한다.
- 미디어 첨부와 외부 책 검색은 저장소, API key, 환경 변수 변경이 필요하므로 구현 전 별도 승인이 필요하다.

## 기술 스택

### Frontend

- React 19
- React Router
- Vite
- Tailwind CSS

### Backend

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- MySQL
- H2 test
- springdoc-openapi

## 실행 방법

### Backend

환경 파일 초기화:

```powershell
cd backend
.\scripts\init-env.ps1
```

서버 실행:

```powershell
cd backend
.\gradlew.bat bootRun
```

테스트:

```powershell
cd backend
.\gradlew.bat test
```

### Frontend

개발 서버:

```powershell
cd frontend
npm run dev
```

빌드:

```powershell
cd frontend
npm run build
```

빌드 결과 미리보기:

```powershell
cd frontend
npm run start
```

## 프로젝트 구조

```text
ChaekList/
├── backend/   Spring Boot REST API
├── frontend/  React web app
└── docs/      명령, 아키텍처, 계획 문서
```

## 검증 기준

- frontend 변경: `cd frontend && npm run build`
- backend 변경: `cd backend && .\gradlew.bat test`
- 문서만 변경: 링크와 내용 범위 확인

## 관련 문서

- `docs/commands.md`
- `docs/architecture.md`
- `docs/plan/2026-04-30/follow-up-plan.md`
- `docs/plan/2026-04-30/mvp-gap-and-expansion-check-plan.md`
- `docs/plan/2026-04-30/priority-1-social-expansion-plan.md`

## 핵심 문장

읽을 책을 찾는 시간을 줄이고, 지금 읽기 좋은 교양서를 보여주는 서비스.
