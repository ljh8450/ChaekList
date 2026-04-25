# ChaekList Design Concept

## Purpose

이 문서는 ChaekList 화면을 설계하고 구현할 때 반복해서 참고할 디자인 기준이다.
회원가입/로그인 화면뿐 아니라 홈, 랭킹, 책 상세, 모바일 화면까지 같은 톤과 UX 원칙을 유지한다.

## Core Tone

- 조용함
- 신뢰감
- 지적인 느낌
- 교양 독서 서비스의 인상
- SNS처럼 과하게 자극적이거나 피드 중심으로 보이지 않게 한다.

## Color System

Figma에서는 아래 색상을 Color Style로 등록한다.

### Primary

| Name | Hex | Usage |
| --- | --- | --- |
| Deep Blue | `#1E2A38` | 헤더, 주요 텍스트, 브랜드 영역 |
| Soft Beige | `#F5F3EF` | 전체 배경, 차분한 섹션 배경 |

### Accent

| Name | Hex | Usage |
| --- | --- | --- |
| Warm Orange | `#F59E0B` | 랭킹, 상승률, 급상승 강조 |
| Muted Green | `#4CAF50` | 추천, 긍정 상태, 완료 상태 |

### Gray

| Name | Hex | Usage |
| --- | --- | --- |
| Text | `#111827` | 본문 기본 텍스트 |
| Sub | `#6B7280` | 보조 설명, 메타 정보 |
| Border | `#E5E7EB` | 구분선, 입력창, 카드 테두리 |

## Typography

Figma에서는 아래 텍스트 스타일을 등록한다.

| Style | Font | Size | Weight | Usage |
| --- | --- | --- | --- | --- |
| Title | Pretendard | 24-32px | Bold | 페이지 제목, 주요 섹션 제목 |
| Body | Pretendard | 14-16px | Regular | 본문, 폼 라벨, 일반 UI 텍스트 |
| Sub | Pretendard | 12-13px | Regular | 도움말, 메타 정보, 보조 설명 |

구현 시 웹폰트가 아직 준비되지 않았다면 `Pretendard`, `Arial`, `Helvetica`, `sans-serif` 순서로 fallback을 둔다.

## Figma Page Structure

Figma 파일은 아래 페이지 구조를 기준으로 나눈다.

```text
ChaekList Design
├── Design System
├── Components
├── Desktop Screens
└── Mobile Screens
```

## Component Guidelines

### Book Card

핵심 도서 노출 컴포넌트다. 추천, 랭킹, 급상승 화면에서 재사용한다.

```text
Frame (Vertical)
├── Book Image (120x160)
├── Title
├── Author
├── Tag
└── Stats
```

필수 정보:

- 책 표지
- 제목
- 저자
- 태그: 인문, 경제, 소설, 자기계발 등
- 통계: 조회수, 찜 수 등

Variants:

- `Default`
- `Ranking`: 번호 표시
- `Trending`: 급상승 표시

### Ranking Item

랭킹 페이지와 홈 우측 랭킹 영역에서 사용한다.

```text
[ 1 ]  Book Image  Title
       Author
       Growth Badge
```

UX 기준:

- 1-3위는 시각적으로 더 강하게 강조한다.
- 상승률 Badge를 표시한다.
- 상승률 강조 색상은 `Warm Orange`를 사용한다.

### Recommendation Card

개인화 추천을 보여주는 컴포넌트다.

필수 구조:

```text
당신을 위한 추천
추천 이유
AI / 경제 관심 기반 추천
```

UX 기준:

- 추천 결과만 보여주지 말고, 왜 추천됐는지 함께 보여준다.
- 추천 이유는 짧고 구체적으로 쓴다.
- 추천 관련 긍정 상태는 `Muted Green`을 사용한다.

### Search / Filter Bar

검색과 카테고리 빠른 접근을 제공한다.

```text
[ 검색창 ]
[ 인문 ] [ 경제 ] [ 자기계발 ]
```

UX 기준:

- 검색창은 상단 주요 위치에 둔다.
- 카테고리 필터는 chip 컴포넌트로 만든다.
- 선택된 chip은 배경색과 테두리로 명확히 구분한다.

### Auth Form

회원가입/로그인 화면의 핵심 컴포넌트다.

필수 기능:

- 로그인/회원가입 탭 전환
- 이메일 입력
- 닉네임 입력: 회원가입에서만 표시
- 비밀번호 입력
- 비밀번호 확인: 회원가입에서만 표시
- 제출 중 로딩 상태
- 서버 오류 메시지
- 성공 상태

UX 기준:

- 회원가입은 부담이 낮아야 한다.
- 오류 메시지는 입력창 가까이에 표시한다.
- 로그인 실패와 중복 가입 실패는 명확히 구분한다.
- 회원가입 성공 후에는 가능한 한 즉시 로그인된 상태로 이어진다.

## Desktop Screens

### Home

Desktop frame width: `1440px`

```text
Header
├── Logo
├── Search
└── Login

Main Grid
├── Left: Trend
│   ├── 급상승 책
│   └── 키워드 트렌드
├── Center: Recommendation
│   ├── 오늘의 추천
│   └── 개인화 추천
└── Right: Ranking
    └── TOP 10
```

홈 화면 텍스트:

- 오늘의 추천
- 지금 당신에게 필요한 책
- 급상승 도서
- 지금 사람들이 가장 많이 읽고 있는 책
- 카테고리별 랭킹
- 교양 독서 기준 TOP 리스트

### Ranking Page

구성:

- 카테고리 필터: 전체, 인문, 경제, 소설
- 기간 필터: 일간, 주간, 월간
- 1위-20위 리스트

UX 기준:

- 일간/주간/월간 필터는 segmented control로 만든다.
- 순위, 제목, 저자, 상승률을 한눈에 비교할 수 있게 한다.
- 1-3위는 색상 또는 배지로 강조한다.

### Book Detail Page

구성:

```text
Left
└── Book Cover

Right
├── Title
├── Summary
├── Recommendation Reason
└── Keywords

Bottom
└── Similar Books
```

UX 기준:

- 책 표지는 상세 화면의 첫 시각 신호가 되어야 한다.
- 추천 이유와 키워드는 제목/요약보다 과하게 튀지 않게 보조 정보로 둔다.
- 비슷한 책은 하단에서 자연스럽게 이어지는 탐색 흐름을 만든다.

## Mobile Screens

Mobile frame width: `390px`

핵심 구조:

```text
Top
└── Search

Scroll
├── 오늘의 추천
├── 급상승
└── 카테고리

Bottom Navigation
├── 홈
├── 랭킹
└── 마이
```

UX 기준:

- 모바일에서는 검색과 추천을 먼저 보여준다.
- 인증 화면에서는 폼을 최우선으로 배치한다.
- 하단 네비게이션은 홈, 랭킹, 마이 3개로 단순하게 유지한다.

## Figma Production Rules

### Frame

- Desktop: `1440px`
- Mobile: `390px`

### Auto Layout

기본값:

- Direction: Vertical
- Padding: `16px`
- Gap: `12-24px`

### Componentization

반복되는 UI는 반드시 컴포넌트로 만든다.

Figma 단축키:

```text
Create Component: Ctrl + Alt + K
```

### Variants

예시:

```text
BookCard
├── Default
├── Ranking
└── Trending
```

## Copy Guidelines

바로 사용할 수 있는 UI 문구:

- 오늘의 추천
- 지금 당신에게 필요한 책
- 급상승 도서
- 지금 사람들이 가장 많이 읽고 있는 책
- 카테고리별 랭킹
- 교양 독서 기준 TOP 리스트
- 추천 이유
- AI / 경제 관심 기반 추천
- 최근 읽은 책과 유사한 콘텐츠
- 비슷한 사용자들이 많이 선택한 책

문구 작성 기준:

- 짧고 구체적으로 쓴다.
- 과장된 마케팅 문구를 피한다.
- 추천 이유는 사용자가 납득할 수 있는 근거 중심으로 쓴다.

## Core UX Principles

반드시 유지해야 할 UX 포인트:

- 추천 이유를 표시한다.
- 상승률을 표시한다.
- 교양 필터를 강조한다.
- 검색과 카테고리 접근을 빠르게 제공한다.
- 로그인하지 않아도 탐색은 가능하게 한다.
- 로그인은 개인화 경험을 강화하는 선택지로 보여준다.

## Implementation Notes

프론트엔드 구현 시:

- 기존 React/Vite 구조를 유지한다.
- 새로운 디자인 시스템 라이브러리를 추가하기 전에 기존 CSS/Tailwind 패턴으로 구현 가능한지 먼저 본다.
- 화면은 실제 동작 가능한 UI를 우선한다.
- 장식적인 랜딩 페이지보다 홈, 랭킹, 상세, 인증의 사용 흐름을 우선한다.

백엔드 연동 시:

- `users` 테이블 DDL 기준으로 회원가입/로그인을 구현한다.
- 추천/랭킹/트렌드 영역은 실제 API가 준비되기 전까지 명확한 mock 상태로 분리한다.
- 추후 API가 붙어도 컴포넌트 구조가 크게 바뀌지 않게 데이터 형태를 단순하게 유지한다.
