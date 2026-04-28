# 후속 구현 계획: 서비스 고유 기능 강화

## 기준

- 작성일: 2026-04-28
- 상위 계획: `docs/plan/2026-04-28/follow-up-plan.md`
- 대상 항목: `2순위: 서비스 고유 기능 강화`
- README 기준: ChaekList의 핵심 차별점은 교양 전용 랭킹, 개인 맞춤 추천, 트렌드 피드다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 현재 확인한 구조

- `books`는 `is_general_eligible`, `filter_status`, `filter_reason`을 가진다.
- `Book`은 `tag()`로 `filter_reason` 또는 "교양 필터 통과"를 반환한다.
- 책 상세는 추천 이유와 비슷한 책을 표시한다.
- `book_keywords`, `categories`, `book_ranking_snapshots`가 존재해 카테고리, 키워드, 랭킹 지표를 설명 근거로 쓸 수 있다.
- 난이도, 독서 분위기, 제외 키워드 상세 목록을 위한 전용 컬럼은 현재 확인되지 않았다.

## 목표

- ChaekList가 단순 책 목록이 아니라 "읽을 책을 고르는 판단 도구"처럼 보이게 한다.
- 교양 필터 리포트, 추천 근거 카드, 읽을 책 결정 보조를 기존 데이터로 가능한 범위부터 제공한다.
- 데이터 출처가 없는 항목은 추정 문구로 단정하지 않는다.

## 1차 구현 범위

1차 구현은 새 schema 없이 가능한 항목에 집중한다.

- 교양 필터 리포트
  - 교양 필터 상태
  - 교양서로 노출된 이유
  - 대표 카테고리
  - 주요 키워드
- 추천 근거 카드
  - 관심 분야와의 연결
  - 읽은 책 또는 저장한 책과의 연결
  - 공통 키워드
  - 교양 필터 통과 근거
- 읽을 책 결정 보조
  - 이 책이 맞을 수 있는 사용자
  - 비슷한 책과의 차이

1차 제외:

- 읽기 난이도 단정
- 예상 독서 분위기 단정
- 제외 키워드가 "없다"는 단정
- ML 기반 교양 분류 리포트

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookDetailResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookSummaryResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/entity/Book.java`
- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`

작업 내용:

1. 책 상세 응답에 추가 설명 블록이 필요한지 확인한다.
2. 1차 구현에서는 상세 응답에만 고유 기능 정보를 추가하는 방향을 우선 검토한다.
3. `filterReport` 후보 필드를 설계한다.
4. `recommendationEvidence` 후보 필드를 설계한다.
5. `readingGuide` 후보 필드를 설계한다.
6. 기존 `Book`의 카테고리, 키워드, `filterReason`, 비슷한 책 결과로 문구를 만든다.
7. 데이터 출처가 없으면 `null` 또는 빈 배열로 내려 frontend가 해당 블록을 숨기게 한다.

응답 구조 후보:

```json
{
  "filterReport": {
    "status": "INCLUDED",
    "reason": "교양 필터 통과",
    "category": "인문",
    "keywords": ["AI", "도파민"]
  },
  "recommendationEvidence": [
    {
      "type": "CATEGORY",
      "label": "관심 분야",
      "description": "인문 분야 책을 찾는 사용자에게 맞는 후보입니다."
    }
  ],
  "readingGuide": {
    "fit": "인문 분야의 최신 흐름을 가볍게 탐색하려는 사용자에게 맞습니다.",
    "similarityNote": "비슷한 책과 카테고리는 같지만 키워드 구성이 다릅니다."
  }
}
```

주의:

- `filter_status`가 `PENDING`인 책은 사용자에게 노출할 문구를 조심스럽게 처리한다.
- 수험서, 문제집, 전공서 성격이 낮다는 문구는 실제 제외 키워드 분석 결과가 없으면 쓰지 않는다.
- 요약 응답까지 확장하면 홈/랭킹 카드가 무거워질 수 있으므로 상세 화면 우선이 안전하다.

## frontend 구현 계획

대상 후보:

- `frontend/src/pages/BookDetailPage.jsx`
- `frontend/src/components/BookCard.jsx`
- `frontend/src/pages/HomePage.jsx`
- `frontend/src/pages/RankingsPage.jsx`

작업 내용:

- 책 상세에 교양 필터 리포트 영역을 추가한다.
- 추천 근거 카드는 상세 화면의 추천 이유 근처에 배치한다.
- 읽을 책 결정 보조는 상세 화면 하단의 비슷한 책 영역과 충돌하지 않게 배치한다.
- 홈/랭킹 카드에는 기존 태그와 추천 이유를 유지하고, 1차에서는 상세로 진입했을 때 근거를 확장한다.
- 응답에 추가 블록이 없으면 기존 화면이 그대로 동작하게 한다.

화면 구성 기준:

- 교양 필터 리포트: 상태, 대표 카테고리, 키워드 중심의 짧은 정보
- 추천 근거 카드: 2~3개 이하의 근거
- 읽을 책 결정 보조: 한 문단 또는 2개 짧은 항목

## 구현 순서

1. 책 상세에만 적용할 응답 구조를 먼저 확정한다.
2. 현재 DB로 표현 가능한 필드와 불가능한 필드를 분리한다.
3. `BookDetailResponse`에 선택적 필드를 추가한다.
4. `BookService`에서 기존 데이터 기반 문구를 생성한다.
5. frontend 상세 화면에 새 섹션을 추가하되 기존 추천 이유와 비슷한 책 흐름을 유지한다.
6. 데이터가 없는 경우 섹션을 숨기는 fallback을 구현한다.
7. backend/frontend 검증 명령을 실행한다.

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

수동 확인 항목:

- 책 상세에서 교양 필터 리포트가 `filter_reason`, 카테고리, 키워드를 반영한다.
- 데이터가 없는 책도 상세 화면이 깨지지 않는다.
- 추천 근거 카드가 현재 추천 이유와 모순되지 않는다.
- 난이도와 분위기처럼 출처가 없는 정보가 단정적으로 노출되지 않는다.
- 모바일에서 새 섹션이 기존 상세 액션 버튼과 겹치지 않는다.

## 제외 범위

- 새 DB 컬럼 추가
- 별도 교양 필터 분석 엔진 구현
- 난이도/분위기 자동 산출
- 트렌드 피드 전면 개편
- 홈/랭킹 카드 전체 리디자인

## 리스크와 후속 작업

- 기존 `filter_reason` 품질이 낮으면 교양 필터 리포트도 설득력이 약하다.
- 추천 근거 카드가 추천 이유와 중복되면 화면만 복잡해질 수 있다.
- 난이도와 분위기는 별도 데이터 수집 기준이 정해진 뒤 2차로 다루는 것이 안전하다.
- 장기적으로는 제외 키워드 매칭 결과와 필터 판단 로그를 별도 저장해야 리포트 품질이 올라간다.
