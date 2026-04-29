# 2026-04-30 후속 구현 계획

## 기준

- 작성일: 2026-04-30
- 최신화일: 2026-04-30
- 역할: docs
- 범위: 남은 구현 계획 문서화만 수행한다.
- 코드 변경, API 변경, DB schema 변경은 이 문서 작업에 포함하지 않는다.

## 정리 결과

구현 완료로 보고 삭제한 계획:

- `priority-1-reading-growth-gamification-plan.md`
- `gamification-expansion-plan.md`

중복된 구식 계획으로 보고 삭제한 계획:

- `sns-expansion-plan.md`

현재 남은 상세 계획:

- `docs/plan/2026-04-30/priority-1-social-expansion-plan.md`
- `docs/plan/2026-04-30/mvp-gap-and-expansion-check-plan.md`

## 남은 구현 계획

### 1순위: SNS 기능 확장과 Header 통합 검색

목표:

- 사용자가 독서 활동과 성장 카드를 선택적으로 공유할 수 있는 공개 피드를 구현한다.
- Header 검색 input을 실제 동작하게 만들고, 책/저자/키워드/공개 게시물/공개 유저를 찾는 통합 검색 진입점을 만든다.
- 자유 게시글보다 구조화된 책/성장 카드 공유를 우선해 moderation 부담을 낮춘다.
- 공개 범위와 개인정보 노출 정책을 먼저 확정한 뒤 feed, like, public profile을 단계적으로 추가한다.

상세 계획:

- `docs/plan/2026-04-30/priority-1-social-expansion-plan.md`

### 후속 확인: MVP 확장 잔여 항목

목표:

- 내부 DB 기반 책 검색, 독서 목적 저장, 개인 독서 성장 카드는 구현된 상태로 정리한다.
- 로컬 DB에 없는 책을 카카오 도서 검색으로 찾아 선택 저장하는 흐름은 별도 확장으로 남긴다.
- 추천 산식 상수화, 외부 책 초기 필터 정책, 운영 데이터 갱신 정책은 구현 전 확인 항목으로 유지한다.

상세 문서:

- `docs/plan/2026-04-30/mvp-gap-and-expansion-check-plan.md`

## 권장 구현 순서

1. Header 검색 input 연결과 검색 결과 페이지 추가
2. 책/키워드 검색 범위 확장
3. 공개 범위/개인정보 정책 확정
4. 구조화된 공유 게시글과 공개 피드
5. 좋아요와 공개 프로필
6. 신고/차단 등 moderation 최소 정책
7. 카카오 외부 책 검색과 선택 저장 확장

## 검증 계획

문서만 변경할 때:

- 별도 빌드 검증은 필요하지 않다.
- 삭제된 상세 계획 링크가 남아 있지 않은지 확인한다.
- 이전 날짜 디렉토리가 삭제되었는지 확인한다.

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

frontend/backend를 함께 변경할 때는 두 검증 명령을 모두 실행한다.

## 남은 리스크

- SNS 확장은 공개 범위, 삭제/비공개 전환, 신고/차단 정책이 확정되지 않으면 구현 리스크가 크다.
- Header 통합 검색은 비공개 독서 기록이나 비공개 프로필을 우회 노출하지 않도록 API 정책을 먼저 고정해야 한다.
- 현재 책 검색은 내부 DB의 제목/저자 기반 검색이다. 키워드 검색, 카카오 외부 도서 검색, 검색 인덱스는 별도 확인이 필요하다.
- 추천 이유는 여전히 `recommendations.reason`의 길이 제한 안에서 유지해야 한다.
