# ChaekList

ChaekList는 수험서, 문제집, 전공서가 섞인 일반 베스트셀러 목록에서 벗어나 진짜 읽을 만한 교양서를 빠르게 발견하도록 돕는 독서 플랫폼입니다.

## 서비스 개요

**한 줄 정의**

교양 독서에 적합한 책을 필터링하고, 사용자의 관심 분야와 독서 목적을 바탕으로 책을 추천하는 웹 서비스입니다.

**핵심 가치**

- 수험서, 문제집, 전공서 중심의 판매량 랭킹에서 벗어나 교양 독서용 책을 더 쉽게 찾게 합니다.
- 비로그인 사용자도 홈에서 인기 책, 급상승 책, 카테고리별 랭킹을 탐색할 수 있습니다.
- 로그인 사용자는 관심 분야, 읽은 책, 저장한 책, 독서 목적을 기반으로 개인화 추천을 받을 수 있습니다.
- 마이페이지에서 읽은 책, 저장한 책, 추천 히스토리, 독서 목적, 독서 성장 상태를 관리할 수 있습니다.

## 현재 구현 상태

### 홈과 랭킹

- 공개 홈: 오늘의 추천, 인기 책, 급상승 책, 카테고리별 랭킹 제공
- 개인화 홈: 로그인 사용자의 관심 분야, 읽은 책, 저장한 책, 독서 목적을 반영한 추천 제공
- 랭킹 페이지: 전체/카테고리별 랭킹과 기간 필터 제공
- 키워드 트렌드: 트렌드 키워드와 대표 도서 제공

### 책 탐색

- 책 상세: 요약, 추천 이유, 비슷한 책, 교양 필터 리포트, 추천 근거, 독서 가이드 제공
- 내부 DB 검색: `GET /api/books/search`로 교양 필터를 통과한 책을 제목/저자로 검색
- 온보딩/마이페이지 책 추가: `BookSearchPanel`을 통해 읽은 책과 저장한 책을 검색 후 추가
- 책 상호작용: `READ`, `SAVE`, `UNSAVE`, `DISMISS` 기록

### 온보딩과 마이페이지

- 온보딩에서 관심 분야, 읽은 책, 독서 목적 선택
- 독서 목적은 1~3개 선택 가능
- 마이페이지에서 관심 분야, 독서 목적, 읽은 책, 저장한 책, 추천 히스토리 확인
- 저장한 책을 읽은 책으로 전환하거나 저장 취소 가능
- 관심 없는 책은 추천 후보에서 제외 가능

### 개인 독서 성장

- 마이페이지 응답에 `readingGrowth` 포함
- 월간 읽은 책 수, 저장 후 읽음 전환 수, 카테고리 다양성, 추천 전환 수 계산
- 대표 배지와 기본 배지 계산
- Header에 로그인 사용자의 대표 배지 표시
- 마이페이지에 독서 성장 카드와 데이터 부족 상태 표시

### 인증

- 회원가입, 로그인, 로그아웃
- access token 기반 인증
- 로그인 상태에 따라 개인화 홈, 마이페이지, 책 상호작용 제공

## 다음 우선순위

### 1순위: Header 통합 검색

현재 Header 검색 input은 UI만 있고 실제 검색 동작은 연결되어 있지 않습니다.

구현 목표:

- Header 검색 input에서 Enter 또는 검색 버튼으로 검색 결과 페이지 이동
- 책/저자/키워드/공개 게시물/공개 유저 통합 검색
- 기존 책 검색 로직 재사용
- 최소 2자 검색어 제한과 빈 결과 안내
- 비공개 독서 기록과 비공개 프로필 미노출

후보 API:

- `GET /api/search?query={query}&type={all|books|keywords|posts|users}&limit={limit}`
- `GET /api/search/books?query={query}&limit={limit}`
- `GET /api/search/posts?query={query}&limit={limit}`
- `GET /api/search/users?query={query}&limit={limit}`

### 2순위: SNS 기능 확장

SNS는 자유 게시판보다 구조화된 독서 활동 공유로 시작합니다.

구현 목표:

- 공개 범위 정책 설계
- 구조화된 공유 게시글 저장
- 공개 피드 조회
- 좋아요/좋아요 취소
- 공개 프로필 기본 응답
- 본인 게시글 비공개 전환 또는 삭제
- 독서 성장 카드, 읽은 책, 저장한 책, 추천받은 책 공유

1차 제외 범위:

- 자유 텍스트 게시글
- 댓글, 대댓글
- 팔로우, 친구 추천, DM
- 이미지 업로드 게시글
- 실시간 알림
- 공개 랭킹
- 자동 moderation

SNS 관련 상세 계획은 `docs/plan/2026-04-30/priority-1-social-expansion-plan.md`를 기준으로 합니다.

### 후속 확장

- 카카오 도서 검색으로 로컬 DB에 없는 책 선택 저장
- 키워드 검색 범위 확장
- 추천 산식 상수화와 테스트 보강
- 운영 랭킹 snapshot 갱신 정책 정리
- 신고/차단 등 moderation 최소 정책

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
- `POST /api/me/onboarding`
- `POST /api/me/books/{bookId}/interactions`
- `GET /api/me/reading-growth/primary-badge`

### Auth

- 회원가입, 로그인, 토큰 기반 인증 API

## 화면 구조

### 홈

- 현재 인기 책
- 오늘의 추천
- 급상승 책
- 키워드 트렌드
- 카테고리별 랭킹

### 랭킹

- 전체 랭킹
- 카테고리별 랭킹
- 기간 필터

### 책 상세

- 책 기본 정보
- 교양 필터 리포트
- 추천 근거
- 독서 가이드
- 비슷한 책
- 저장/읽음/관심 없음 액션

### 온보딩

- 관심 분야 선택
- 독서 목적 선택
- 읽은 책 검색 및 선택

### 마이페이지

- 프로필 요약
- 독서 성장 카드
- 관심 분야
- 독서 목적
- 읽은 책
- 저장한 책
- 추천 히스토리

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
- H2 테스트
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

- frontend 변경 후: `cd frontend && npm run build`
- backend 변경 후: `cd backend && .\gradlew.bat test`
- 문서만 변경한 경우: 링크와 변경 내용 확인

## 관련 문서

- `docs/commands.md`
- `docs/architecture.md`
- `docs/plan/2026-04-30/follow-up-plan.md`
- `docs/plan/2026-04-30/mvp-gap-and-expansion-check-plan.md`
- `docs/plan/2026-04-30/priority-1-social-expansion-plan.md`

## 핵심 문장

읽을 책을 찾는 시간을 줄이고, 지금 읽기 좋은 교양서를 보여주는 서비스
