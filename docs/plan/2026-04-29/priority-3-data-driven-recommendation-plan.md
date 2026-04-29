# 후속 구현 계획: 데이터 기반 추천 확장

## 기준

- 작성일: 2026-04-29
- 상위 계획: `docs/plan/2026-04-29/follow-up-plan.md`
- 대상 항목: `3순위: 데이터 기반 추천 확장`
- README 기준: 랭킹은 조회수, 클릭률, 저장 수, 리뷰 수, 최근 상승률을 반영하고, 추천은 콘텐츠 기반에서 협업 필터링으로 확장한다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 현재 확인한 구조

- `BookRankingSnapshot`은 `viewCount`, `clickCount`, `saveCount`, `reviewCount`, `recentGrowthRate`, `rankingScore`를 가진다.
- 랭킹 API는 최신 snapshot 기준으로 전체, 카테고리, 급상승 목록을 반환한다.
- 홈 개인화 추천은 관심 분야, 읽은 책, 저장한 책의 카테고리/키워드 기반으로 점수를 계산한다.
- 개인 추천은 `recommendations`에 `CONTENT_BASED` 유형으로 저장된다.
- `READ`, `DISMISS` 책은 개인 추천 후보에서 제외한다.
- 저장 취소는 `SAVE`와 이후 `UNSAVE` 이벤트를 비교해 최신 저장 상태를 판단한다.

## 목표

- 현재 규칙 기반 추천을 행동 데이터 기반으로 점진 확장한다.
- 랭킹, 유사 도서, 개인 추천의 점수 산식을 명확히 문서화하고 테스트 가능하게 만든다.
- README의 트렌드 피드와 개인 맞춤 추천을 장기적으로 강화한다.

## 1차 구현 범위

- 현재 수집 중인 행동 이벤트와 랭킹 지표 정리
- 추천 점수 산식 분리
- 유사 도서 점수 산식 개선
- 후보 제외 정책 정리
- 추천 결과 검증 기준 문서화

1차 제외:

- 유사 사용자 추천 본 구현
- ML 모델 학습 파이프라인
- 리뷰 감정 분석
- 외부 analytics 도구 연동
- 실시간 스트리밍 추천

## 데이터 점검 계획

행동 이벤트:

- `VIEW`
- `CLICK`
- `SAVE`
- `UNSAVE`
- `READ`
- `DISMISS`

현재 코드에서 확인된 활용:

- `SAVE`, `UNSAVE`: 저장 상태 판단
- `READ`: 읽은 책 기반 추천과 후보 제외
- `DISMISS`: 개인 추천 후보 제외
- `VIEW`, `CLICK`: 일부 마이페이지 쿼리에서 조회되지만 추천 산식의 핵심 입력으로는 아직 제한적

랭킹 지표:

- `view_count`
- `click_count`
- `save_count`
- `review_count`
- `recent_growth_rate`
- `ranking_score`

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/entity/BookRankingSnapshot.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/repository/BookRankingSnapshotRepository.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/test/java/com/example/chaeklist/BookControllerTest.java`
- `backend/src/test/java/com/example/chaeklist/MyPageControllerTest.java`

작업 순서:

1. 현재 추천 점수 계산을 작은 메서드 단위로 분리한다.
2. 관심 분야, 읽은 책, 저장한 책, 공통 키워드 가중치를 상수화한다.
3. 행동 이벤트 기반 가중치를 추가할지 결정한다.
4. `DISMISS`, `READ` 후보 제외 정책을 별도 메서드로 명확히 둔다.
5. 유사 도서 추천에서 카테고리 일치, 공통 키워드 수, 저장 수 또는 랭킹 지표를 분리해 점수화한다.
6. 랭킹 snapshot 기반 점수가 개인 추천 fallback에 어떻게 반영되는지 정리한다.
7. 추천 reason 생성은 점수 요인과 모순되지 않도록 함께 정리한다.
8. 후보 부족 시 fallback 정책을 테스트한다.

개인 추천 점수 산식 초안:

- 관심 분야 카테고리 일치: +50
- 읽은 책 카테고리 일치: +25
- 저장한 책 카테고리 일치: +20
- 읽은 책 키워드 일치: 키워드당 +10
- 저장한 책 키워드 일치: 키워드당 +8
- 최근 저장 수 또는 랭킹 점수 보조: +0~10
- `READ`, `DISMISS` 책: 후보 제외

유사 도서 점수 산식 초안:

- 같은 카테고리: 필수 조건 또는 +30
- 공통 키워드: 키워드당 +15
- 최신 랭킹 점수: 정규화 후 +0~10
- 저장 수: 정규화 후 +0~5

랭킹 점수 점검 초안:

- 현재 snapshot의 `ranking_score`가 이미 계산된 값이면 API에서는 재계산하지 않는다.
- 재계산이 필요하면 별도 배치/수집 계획으로 분리한다.
- 급상승은 `recent_growth_rate` 기준을 유지하되, 동률일 때 저장 수나 ranking score를 보조 정렬로 검토한다.

## frontend 구현 계획

대상 후보:

- `frontend/src/pages/HomePage.jsx`
- `frontend/src/pages/BookDetailPage.jsx`
- `frontend/src/pages/RankingsPage.jsx`
- `frontend/src/pages/MyPage.jsx`

작업 순서:

1. 홈 오늘의 추천 reason이 새 점수 요인과 맞는지 문구를 확인한다.
2. 상세 추천 근거 카드가 개인 추천 근거처럼 과장되지 않도록 유지한다.
3. 랭킹 페이지에서 기간, 카테고리, 성장률 표기가 새 정렬 기준과 모순되지 않는지 확인한다.
4. 마이페이지 추천 히스토리에서 source와 score 표시가 이해 가능한지 검토한다.
5. 추천 후보가 부족할 때 fallback 문구를 유지한다.

UI 원칙:

- 점수 산식 자체를 사용자에게 그대로 노출하지 않는다.
- "왜 추천됐는지"는 짧고 검증 가능한 근거만 표시한다.
- 데이터가 없는 경우 추정 문구를 만들지 않는다.

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

- 신규 사용자 fallback 추천
- 관심 분야 기반 추천
- 읽은 책 키워드 기반 추천
- 저장한 책 키워드 기반 추천
- `READ` 책 후보 제외
- `DISMISS` 책 후보 제외
- 후보 부족 시 랭킹 fallback
- 추천 히스토리 저장 reason과 화면 reason 일치

## 지표 검증 계획

추천 품질을 바로 자동 판단하기 어렵기 때문에 구현 후 다음 지표를 추적한다.

- 추천 카드 클릭률
- 추천 책 저장률
- 추천 책 읽음 전환율
- 관심 없음 처리율
- 추천 히스토리 재방문율
- 카테고리별 후보 부족 비율

## 리스크와 후속 작업

- 사용자 행동 데이터가 적으면 데이터 기반 가중치가 추천 품질을 오히려 낮출 수 있다.
- `VIEW`, `CLICK` 이벤트가 충분히 저장되지 않으면 랭킹 개선에 활용하기 어렵다.
- 유사 사용자 추천은 개인정보, 데이터 희소성, 성능 문제가 있어 별도 계획이 필요하다.
- ML 기반 추천은 Python/FastAPI 추천 서비스와의 경계가 정리된 뒤 진행하는 것이 안전하다.
