# 개인 독서 성장/게이미피케이션 상세 구현 계획

## 기준

- 작성일: 2026-04-29
- 상위 계획: `docs/plan/2026-04-29/follow-up-plan.md`
- 기존 참고 계획: `docs/plan/2026-04-29/gamification-expansion-plan.md`
- 역할: planner
- 범위: 계획 문서화만 수행한다. 코드, API, DB schema 변경은 포함하지 않는다.

## 목표

- 마이페이지에서 개인 독서 성장 상태를 한눈에 보여준다.
- 권수 경쟁 대신 취향 발견, 분야 확장, 추천 전환 같은 개인 변화 중심 지표를 제공한다.
- 1차 구현은 별도 schema 없이 현재 데이터로 계산 가능한 응답 필드를 추가하는 방향으로 제한한다.

## 현재 기반

- `user_book_interactions`에는 `READ`, `SAVE`, `UNSAVE`, `DISMISS`와 일부 `VIEW`, `CLICK` 기반 집계가 있다.
- `recommendations`에는 개인화 추천 히스토리와 점수가 저장된다.
- 마이페이지는 관심 분야, 독서 목적, 읽은 책, 저장한 책, 추천 히스토리를 이미 응답한다.
- 독서 목적과 추천 산식은 구현되어 있어 목적별 진행도 계산의 기반으로 사용할 수 있다.

## 1차 구현 범위

- 마이페이지 응답에 계산형 `readingGrowth` 필드 추가
- 월간 읽은 책 수, 저장 후 읽음 전환 수, 읽은 책 카테고리 다양성 계산
- 성장 레벨과 진행률 계산
- 기본 배지 3~5개 계산
- 데이터가 부족한 사용자용 빈 상태 응답
- 마이페이지 성장 카드 UI 추가

## 1차 제외 범위

- 성장 점수/배지 획득 이력 영구 저장
- 공개 랭킹, 리그, 친구 경쟁
- streak, 알림, 보상 재화
- SNS 공유
- 신규 schema 추가

## backend 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/mypage/service/MyPageService.java`
- `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/MyPageResponse.java`
- 신규 후보: `backend/src/main/java/com/example/chaeklist/domain/mypage/dto/ReadingGrowthResponse.java`
- `backend/src/test/java/com/example/chaeklist/MyPageControllerTest.java`

작업 순서:

1. `ReadingGrowthResponse` 응답 구조를 정의한다.
2. `MyPageResponse`에 `readingGrowth` 필드를 추가한다.
3. `MyPageService`에서 현재 사용자 기준 성장 지표를 계산한다.
4. 월간 읽은 책 수는 `READ.created_at` 기준으로 계산한다.
5. 저장 후 읽음 전환 수는 동일 책에 대해 `SAVE` 이후 `READ`가 있는 경우로 계산한다.
6. 카테고리 다양성은 읽은 책의 distinct category 수로 계산한다.
7. 추천 전환 수는 `recommendations`에 존재하는 책을 이후 `SAVE` 또는 `READ`한 경우로 계산한다.
8. 성장 레벨은 계산 점수 구간으로 산출하되, UI에는 점수보다 레벨명과 진행률을 노출한다.
9. 배지는 조건 충족 여부만 계산해 응답한다.
10. 테스트에 신규 사용자, 읽은 책 보유 사용자, 저장 후 읽음 전환 사용자, 여러 카테고리 사용자 케이스를 추가한다.

응답 구조 후보:

```json
{
  "readingGrowth": {
    "level": 2,
    "levelName": "취향 발견",
    "progressPercent": 45,
    "summary": "경제 분야와 투자 키워드를 중심으로 취향이 선명해지고 있습니다.",
    "monthlyReadCount": 3,
    "savedToReadCount": 1,
    "categoryDiversityCount": 2,
    "recommendationConversionCount": 1,
    "badges": [
      {
        "code": "ECONOMY_STARTER",
        "label": "경제 입문자",
        "description": "경제/투자 분야 책을 꾸준히 읽고 있습니다."
      }
    ]
  }
}
```

레벨 후보:

- Level 1: 탐색 시작
- Level 2: 취향 발견
- Level 3: 분야 확장
- Level 4: 독서 루틴 형성
- Level 5: 교양 탐험가

점수 후보:

- 읽은 책 1권: +10
- 저장한 책을 읽음으로 전환: +12
- 새 카테고리 첫 읽음: +15
- 독서 목적과 맞는 책 읽음: +10
- 추천받은 책 저장: +5
- 추천받은 책 읽음: +15

배지 후보:

- `FIRST_READ`: 읽은 책 1권 이상
- `CATEGORY_EXPLORER`: 서로 다른 카테고리 3개 이상 읽음
- `SAVED_TO_READ`: 저장한 책 1권 이상을 읽음 처리
- `RECOMMENDATION_FOLLOWER`: 추천받은 책을 저장 또는 읽음
- `PURPOSE_MATCH`: 선택한 독서 목적과 맞는 책 3권 이상 읽음

## frontend 계획

대상 후보:

- `frontend/src/pages/MyPage.jsx`
- 필요 시 신규 컴포넌트: `frontend/src/components/ReadingGrowthCard.jsx`

작업 순서:

1. 마이페이지 상단 요약 아래에 독서 성장 카드를 배치한다.
2. 레벨명, 진행률, 이번 달 읽은 책, 저장 후 읽음 전환, 카테고리 다양성을 표시한다.
3. 배지는 2~4개까지만 노출하고, 없는 경우 읽은 책 추가 또는 저장한 책 읽음 처리로 연결한다.
4. 숫자 경쟁처럼 보이지 않도록 문구는 성장 설명 중심으로 작성한다.
5. 모바일에서 카드가 과도하게 길어지지 않도록 요약 지표를 2열 이하로 정리한다.
6. 데이터가 없는 사용자는 빈 상태와 다음 행동 링크를 표시한다.

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

- 신규 사용자 빈 상태
- 읽은 책 1권만 있는 사용자
- 여러 카테고리를 읽은 사용자
- 저장한 책을 읽음 처리한 사용자
- 추천받은 책을 저장/읽음 처리한 사용자
- 모바일 마이페이지 레이아웃

## 주의 사항

- 성장 점수는 내부 계산값으로 두고 과도하게 경쟁적으로 노출하지 않는다.
- 배지 이력을 저장하려면 schema 변경이 필요하므로 2차로 분리한다.
- `READ`가 실제 완독인지 단순 읽은 책 표시인지 의미를 확정해야 한다.
- 추천 전환 계산은 추천 생성 시각과 상호작용 시각 비교가 필요하다.
