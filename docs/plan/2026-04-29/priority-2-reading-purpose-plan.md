# 후속 구현 계획: 독서 목적 저장과 추천 반영

## 기준

- 작성일: 2026-04-29
- 상위 계획: `docs/plan/2026-04-29/follow-up-plan.md`
- 대상 항목: `2순위: 독서 목적 저장과 추천 반영`
- README 기준: 개인 맞춤 추천은 사용자의 관심 분야, 읽은 책, 유사한 사용자 행동을 바탕으로 "나에게 맞는 책"을 빠르게 발견하도록 돕는다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 현재 확인한 구조

- 온보딩 저장 요청은 `categoryIds`, `readBookIds`만 포함한다.
- 관심 분야는 `user_interest_categories`에 저장된다.
- 읽은 책과 저장한 책은 `user_book_interactions`에 `READ`, `SAVE` 등으로 저장된다.
- 홈 개인화 추천은 관심 분야, 읽은 책, 저장한 책의 카테고리/키워드를 기반으로 점수를 계산한다.
- 추천 히스토리는 `recommendations`에 저장되며 reason은 `VARCHAR(255)` 제약을 가진다.
- 독서 목적 전용 schema는 현재 확인되지 않았다.

## 목표

- 사용자가 책을 찾는 목적을 온보딩에서 선택하고 마이페이지에서 관리할 수 있게 한다.
- 독서 목적을 추천 후보 점수와 추천 이유에 반영한다.
- 목적 미선택 사용자도 기존 추천 흐름이 깨지지 않게 한다.

## 독서 목적 후보

- 지식 확장
- 자기계발
- 경제/투자 이해
- 가벼운 독서
- 트렌드 파악

후보 코드는 UI 문구와 분리한다.

- `KNOWLEDGE`
- `SELF_IMPROVEMENT`
- `ECONOMY_INVESTING`
- `LIGHT_READING`
- `TREND_TRACKING`

## 1차 구현 범위

- 독서 목적 후보 정의
- 독서 목적 저장 위치 결정
- 온보딩 독서 목적 선택 UI
- 마이페이지 독서 목적 조회/수정 UI
- 홈 개인화 추천 점수와 reason에 독서 목적 반영

1차 제외:

- ML 기반 목적 추론
- 목적별 별도 추천 모델
- 목적 변경 이력 저장
- 목적별 푸시/알림
- 목적별 상세 통계

## schema 검토 계획

DB schema 변경 가능성이 높으므로 구현 전 별도 승인이 필요하다.

후보 1: `user_reading_purposes`

```sql
CREATE TABLE user_reading_purposes (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  purpose_code VARCHAR(50) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE (user_id, purpose_code)
);
```

장점:

- 다중 선택을 자연스럽게 지원한다.
- 관심 분야 저장 구조와 유사하다.

주의:

- purpose master table을 둘지 enum code만 둘지 결정해야 한다.

후보 2: `user_profiles.reading_purpose`

장점:

- 단일 선택이면 단순하다.

주의:

- 현재 구조에서 user profile 테이블 존재 여부를 확인해야 한다.
- 다중 목적 확장성이 낮다.

권장:

- 1~3개 다중 선택을 고려해 `user_reading_purposes` 별도 테이블을 우선 검토한다.

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/OnboardingRequest.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/OnboardingOptionsResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/MyPageResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`
- `backend/src/test/java/com/example/chaeklist/MyPageControllerTest.java`

API/DTO 변경 후보:

```json
{
  "categoryIds": [1, 2],
  "readBookIds": [301],
  "readingPurposeCodes": ["KNOWLEDGE", "TREND_TRACKING"]
}
```

작업 순서:

1. 독서 목적 저장 schema 변경 필요성을 확정하고 승인받는다.
2. 독서 목적 후보 응답 DTO를 추가한다.
3. `/api/me/onboarding-options`에 독서 목적 후보를 포함한다.
4. `OnboardingRequest`에 `readingPurposeCodes`를 추가한다.
5. 저장 시 기존 사용자 목적을 교체 저장한다.
6. `/api/me/mypage` 응답에 현재 독서 목적을 포함한다.
7. `BookService.getPersonalRecommendation`에서 목적 데이터를 조회한다.
8. 목적과 카테고리/키워드 매핑을 규칙 기반으로 반영한다.
9. 추천 reason 문구를 255자 이하로 유지한다.
10. 저장/조회/추천 반영 테스트를 추가한다.

목적별 매핑 초안:

- `KNOWLEDGE`: 인문, 경제, AI, 역사, 사회
- `SELF_IMPROVEMENT`: 자기계발, 습관, 집중, 루틴
- `ECONOMY_INVESTING`: 경제, 투자, 돈, 시장
- `LIGHT_READING`: 에세이, 소설, 일상, 관계
- `TREND_TRACKING`: AI, 도파민, 트렌드, 기술, 투자

추천 점수 반영 초안:

- 목적과 카테고리 일치: +20
- 목적과 키워드 일치: 키워드당 +8
- 관심 분야/읽은 책/저장한 책 점수보다 낮은 보조 가중치로 시작한다.

## frontend 구현 계획

대상 후보:

- `frontend/src/pages/OnboardingPage.jsx`
- `frontend/src/pages/MyPage.jsx`
- `frontend/src/pages/HomePage.jsx`

온보딩 작업:

1. 관심 분야와 읽은 책 사이 또는 관심 분야 아래에 독서 목적 선택 영역을 추가한다.
2. 1~3개 선택 제한을 둔다.
3. 목적 후보는 API 응답을 우선 사용하고, 응답이 없으면 저장을 막거나 안내한다.
4. 저장 요청에 `readingPurposeCodes`를 포함한다.
5. 기존 사용자 수정 모드에서는 마이페이지 응답의 목적을 초기 선택값으로 사용한다.

마이페이지 작업:

1. 좌측 사용자 요약 또는 관심 분야 섹션 아래에 독서 목적 요약을 추가한다.
2. 목적 수정은 온보딩 수정 흐름으로 연결하거나 같은 화면 안에서 처리한다.
3. 목적이 없으면 "독서 목적을 선택하면 추천 이유가 더 구체화됩니다" 문구를 표시한다.

홈 작업:

1. 추천 이유 문구가 목적 기반일 때 과장되지 않게 표시한다.
2. 목적 미선택 사용자는 기존 이유를 그대로 표시한다.

## 검증 계획

backend:

```powershell
cd backend
.\gradlew.bat test
```

frontend:

```powershell
cd frontend
npm run build
```

수동 확인:

- 신규 온보딩에서 목적 선택 후 저장
- 기존 사용자 목적 수정
- 목적 미선택 또는 응답 누락 시 fallback
- 홈 개인화 추천 reason 반영
- 마이페이지 목적 표시
- 선택 개수 제한

## 리스크와 후속 작업

- DB schema 변경이 필요하므로 구현 전 승인 대상이다.
- 목적과 추천 후보 매핑이 임의 규칙으로 보일 수 있어 문구를 조심스럽게 유지해야 한다.
- reason 길이 제한 때문에 목적, 관심 분야, 키워드 근거를 모두 담기 어렵다.
- 장기적으로는 목적별 클릭/저장 성과를 보고 가중치를 조정해야 한다.
