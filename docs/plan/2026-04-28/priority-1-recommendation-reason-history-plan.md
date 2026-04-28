# 후속 구현 계획: 추천 이유 고도화와 추천 히스토리 저장

## 기준

- 작성일: 2026-04-28
- 상위 계획: `docs/plan/2026-04-28/follow-up-plan.md`
- 대상 항목: `1순위: 추천 이유 고도화와 추천 히스토리 저장`
- README 기준: 개인 맞춤 추천은 관심 분야, 읽은 책, 유사한 행동을 바탕으로 "나에게 맞는 책"을 빠르게 발견하게 해야 한다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 현재 확인한 구조

- `BookService`는 홈의 오늘의 추천에서 개인화 후보를 점수화한다.
- 현재 추천 점수 근거는 관심 분야, 읽은 책 카테고리, 저장한 책 카테고리, 읽은 책 키워드, 저장한 책 키워드다.
- `BookSummaryResponse`와 `BookDetailResponse`는 `recommendationReason`을 내려준다.
- `recommendations` 테이블은 `user_id`, `book_id`, `recommendation_type`, `reason`, `score`, `generated_at`을 가진다.
- `MyPageService`는 `recommendations`를 조회해 마이페이지 추천 히스토리로 표시한다.
- 현재 홈 추천 생성 결과가 항상 `recommendations`에 저장되는 흐름은 명확하지 않다.

## 목표

- 추천 이유를 "무엇 때문에 추천됐는지"가 드러나는 문장으로 고도화한다.
- 홈에서 생성된 개인화 추천을 `recommendations`에 저장해 마이페이지에서 재현 가능하게 한다.
- 추천 히스토리가 단순 목록이 아니라 사용자의 관심 분야, 읽은 책, 저장한 책 입력이 반영된 기록처럼 보이게 한다.

## 구현 원칙

- 1차 구현은 현재 DB 구조를 우선 사용한다.
- 새 컬럼 추가가 필요하면 구현 전에 별도 승인받는다.
- 추천 이유는 과장하지 않고 실제 점수 계산에 사용한 근거만 표시한다.
- 같은 사용자, 같은 책, 같은 추천 유형은 기존 unique key 기준으로 중복 저장하지 않는다.
- `DISMISS`된 책과 이미 읽은 책은 추천 저장 대상에서 제외한다.

## 추천 이유 생성 기준

추천 이유는 다음 우선순위로 만든다.

1. 관심 분야와 후보 책의 대표 카테고리가 일치한다.
2. 읽은 책과 후보 책이 공통 키워드를 가진다.
3. 저장한 책과 후보 책이 공통 키워드를 가진다.
4. 읽은 책과 후보 책의 대표 카테고리가 일치한다.
5. 저장한 책과 후보 책의 대표 카테고리가 일치한다.
6. 개인화 근거가 약하면 랭킹과 교양 필터 기준 fallback을 사용한다.

문장 예시:

- "관심 분야로 선택한 경제 분야와 맞고, 투자 키워드를 함께 가진 책입니다."
- "읽은 책과 루틴 키워드를 공유해 다음 독서 후보로 추천합니다."
- "저장한 책과 같은 인문 분야에서 최근 저장이 늘어난 교양서입니다."
- "랭킹 지표와 교양 필터링 기준을 반영한 책입니다."

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookSummaryResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/MyPageRecommendationResponse.java`
- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`
- `backend/src/test/java/com/example/chaeklist/MyPageControllerTest.java`

작업 내용:

1. `BookService`의 개인화 추천 점수 계산 결과에 근거 타입과 근거 값을 함께 남길 수 있게 정리한다.
2. 추천 이유 생성 로직을 점수 계산과 같은 입력값만 사용하도록 맞춘다.
3. 개인화 추천이 선택되면 `recommendations`에 `CONTENT_BASED` 유형으로 저장한다.
4. 이미 같은 `user_id`, `book_id`, `recommendation_type`이 있으면 reason, score, generated_at 갱신 기준을 정한다.
5. fallback 추천은 개인화 근거가 약하므로 1차 구현에서는 저장하지 않거나 `TRENDING` 저장 여부를 별도 판단한다.
6. 마이페이지 추천 히스토리 응답은 기존 필드로 먼저 충분한지 확인한다.
7. 필요 시 `source` 대신 사용자 친화 문구를 frontend에서 매핑하거나 backend에서 표시용 값을 추가하는 방안을 비교한다.

저장 방식 후보:

```sql
INSERT INTO recommendations (user_id, book_id, recommendation_type, reason, score, generated_at)
VALUES (?, ?, 'CONTENT_BASED', ?, ?, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  reason = VALUES(reason),
  score = VALUES(score),
  generated_at = CURRENT_TIMESTAMP;
```

주의:

- 현재 DDL의 `user_book_interactions` check constraint에 `DISMISS`가 포함되는지 실제 DB와 문서가 일치하는지 확인한다.
- `recommendations.reason`은 `VARCHAR(255)`이므로 추천 이유는 짧게 유지한다.
- 홈 조회가 자주 호출될 때 추천 히스토리 시간이 매번 갱신되는 것이 맞는지 제품 기준을 먼저 정한다.

## frontend 구현 계획

대상 후보:

- `frontend/src/pages/HomePage.jsx`
- `frontend/src/pages/MyPage.jsx`
- `frontend/src/pages/BookDetailPage.jsx`
- `frontend/src/components/BookCard.jsx`

작업 내용:

- 홈 오늘의 추천 카드에서 `recommendationReason`을 더 구체적인 이유로 표시한다.
- 마이페이지 추천 히스토리에서 reason, source, generatedAt이 잘 읽히도록 유지한다.
- 빈 히스토리 상태는 "추천을 만들 입력이 부족함"과 "추천 조회 전"을 구분할 수 있는지 검토한다.
- 추천 이유가 긴 경우 카드 레이아웃이 깨지지 않도록 줄바꿈과 최대 폭을 확인한다.

## 구현 순서

1. 추천 이유와 점수 근거의 우선순위를 확정한다.
2. `PersonalizedRecommendation` 결과에 reason과 score 외 근거 타입이 필요한지 판단한다.
3. 홈 개인화 추천 선택 시 `recommendations` 저장 흐름을 추가한다.
4. 중복 추천 갱신 정책을 정하고 SQL을 구현한다.
5. 마이페이지 추천 히스토리 조회가 저장된 reason을 그대로 보여주는지 확인한다.
6. frontend에서 추천 이유 표시 영역의 줄바꿈과 빈 상태를 확인한다.
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

- 관심 분야 기반 추천 이유가 관심 분야명을 포함한다.
- 읽은 책 기반 추천 이유가 공통 키워드 또는 카테고리를 포함한다.
- 저장한 책 기반 추천 이유가 저장 행동을 근거로 표현된다.
- 홈에서 개인화 추천을 받은 뒤 마이페이지 추천 히스토리에 같은 reason이 남는다.
- 이미 읽었거나 관심 없음 처리한 책은 오늘의 추천과 히스토리 저장 대상에서 제외된다.

## 제외 범위

- 협업 필터링
- 유사 사용자 추천
- 추천 모델용 Python/FastAPI 추가
- 추천 이유 다국어화
- 추천 히스토리 상세 페이지

## 리스크와 후속 작업

- 홈 조회마다 추천 히스토리를 갱신하면 기록성이 약해질 수 있다.
- 추천 이유가 실제 점수 근거와 다르면 신뢰도가 낮아진다.
- 현재 reason 길이가 255자로 제한되어 있어 복합 근거를 길게 설명하기 어렵다.
- 추천 저장 정책을 잘못 잡으면 마이페이지 히스토리가 같은 책으로 반복될 수 있다.
