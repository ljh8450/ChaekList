# 후속 구현 계획: 책 이미지 연동과 캐싱

## 기준

- 작성일: 2026-04-28
- 상위 계획: `docs/plan/2026-04-28/follow-up-plan.md`
- 대상 항목: `2순위: 책 이미지 연동과 캐싱`
- 목적: 책 카드와 책 상세에서 카테고리 색상 블록 대신 실제 책 표지 이미지를 표시할 수 있도록 외부 도서 API에서 이미지 URL을 가져와 DB에 저장한다.
- 범위: planner 역할 문서화만 수행한다. frontend/backend 코드, API, DB schema 변경은 포함하지 않는다.

## 현재 문제

- 현재 frontend의 `BookCard`와 `BookDetailPage`는 실제 이미지가 아니라 카테고리 기반 색상 블록을 사용한다.
- 사용자가 실제 책을 식별하기 어렵고, 서비스의 신뢰감도 낮아질 수 있다.
- 외부 API를 화면에서 직접 호출하면 rate limit, 장애, 응답 속도 문제가 사용자 경험에 바로 노출된다.

## 기본 전략

1차 구현은 외부 도서 API를 사용하되, 화면 요청마다 외부 API를 호출하지 않는다.

반드시 다음 흐름을 따른다.

1. 책 검색 API 호출
2. 응답에서 `thumbnail` 또는 이미지 URL 추출
3. `books.image_url`에 저장
4. 이후 홈, 랭킹, 책 상세, 마이페이지에서는 DB의 `image_url`만 사용
5. `image_url`이 없으면 기존 카테고리 색상 표지 fallback을 유지

## 외부 API 후보

### 1. Kakao Developers 도서 검색 API

예시 응답:

```json
{
  "documents": [
    {
      "title": "사피엔스",
      "thumbnail": "https://.../image.jpg"
    }
  ]
}
```

사용 필드:

- `title`
- `authors`
- `thumbnail`
- 필요 시 `isbn`

장점:

- 구현 속도가 빠르다.
- MVP에 적합하다.
- 한국어 도서 검색 품질이 좋다.
- `thumbnail`이 바로 표지 이미지 URL로 쓰기 쉽다.

단점:

- 외부 의존성이 생긴다.
- API key가 필요하다.
- 호출 제한이 있다.

### 2. 네이버 개발자센터 책 검색 API

장점:

- 한국어 도서 검색 품질이 좋다.
- 국내 서비스와 데이터 매칭이 쉽다.

단점:

- API key가 필요하다.
- 응답 데이터 정책과 표시 조건을 확인해야 한다.

### 3. Google Books API

장점:

- 글로벌 도서 데이터가 풍부하다.
- 일부 사용 방식에서는 접근이 쉽다.

단점:

- 국내 도서 표지 매칭이 약할 수 있다.
- 한국어 도서 우선 서비스에는 부정확한 결과가 섞일 수 있다.

## 추천 선택

1차 구현에서는 Kakao Developers 도서 검색 API를 우선 검토한다.

선정 이유:

- 한국어 도서 검색에 유리하다.
- 응답의 `thumbnail`을 `books.image_url`에 저장하기 쉽다.
- MVP 구현 속도가 빠르다.

대체 기준:

- Kakao API key 준비가 어렵거나 정책상 맞지 않으면 네이버 책 검색 API를 검토한다.
- 해외 도서 확장이 필요할 때 Google Books를 후순위로 검토한다.

## DB 저장 기준

대상 테이블:

- `books`

필요 컬럼 후보:

```text
books
- id
- title
- author
- image_url
```

주의 사항:

- 현재 `books.image_url` 컬럼 존재 여부를 구현 전에 확인한다.
- 컬럼이 없다면 DB schema 변경 승인이 필요하다.
- 컬럼 타입은 일반 URL 저장을 고려해 `VARCHAR(500)` 이상을 검토한다.
- 이미지 파일 자체를 저장하지 않고 URL만 저장한다.

권장 컬럼:

```sql
ALTER TABLE books
ADD COLUMN image_url VARCHAR(500) NULL;
```

단, 이 쿼리는 실제 구현 전에 schema 변경 승인을 받은 뒤 실행한다.

## backend 구현 계획

대상 후보:

- `backend/src/main/java/com/example/chaeklist/domain/book/entity/Book.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookSummaryResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/dto/BookDetailResponse.java`
- `backend/src/main/java/com/example/chaeklist/domain/book/service/BookService.java`
- 새 외부 API client 또는 service

작업 내용:

1. `books.image_url` 컬럼 존재 여부를 확인한다.
2. `Book` projection/entity에서 `imageUrl`을 읽을 수 있게 한다.
3. `BookSummaryResponse`와 `BookDetailResponse`에 `imageUrl` 필드를 추가한다.
4. 외부 API 호출은 화면 요청 경로가 아니라 별도 보강 작업 흐름에서 수행한다.
5. 제목과 저자 기준으로 외부 도서 API를 호출한다.
6. 응답 후보 중 title/author가 가장 잘 맞는 결과의 thumbnail을 선택한다.
7. 선택한 image URL을 `books.image_url`에 저장한다.
8. 저장 실패 또는 검색 실패 시 기존 fallback을 유지한다.

외부 API 호출 위치 후보:

- 관리자/개발용 일괄 보강 command
- backend service 메서드
- 임시 운영용 endpoint

1차 권장:

- 화면 조회 중 자동 호출은 피한다.
- 개발/운영자가 실행할 수 있는 일괄 보강 흐름을 먼저 설계한다.

## frontend 구현 계획

대상 후보:

- `frontend/src/components/BookCard.jsx`
- `frontend/src/pages/BookDetailPage.jsx`
- `frontend/src/pages/HomePage.jsx`
- `frontend/src/pages/MyPage.jsx`

작업 내용:

- 응답의 `imageUrl`이 있으면 `<img>`로 표시한다.
- `imageUrl`이 없거나 이미지 로드에 실패하면 기존 카테고리 색상 블록을 표시한다.
- 책 표지 영역의 크기와 비율은 기존 `BookCard`와 상세 화면의 레이아웃을 유지한다.
- alt 문구는 책 제목을 기준으로 제공한다.

주의 사항:

- 이미지 로드 실패가 레이아웃 깨짐으로 이어지지 않게 한다.
- 외부 이미지 URL을 직접 화면에서 새로 검색하지 않는다.
- 카드와 상세 화면의 fallback 스타일은 현재 디자인 톤을 유지한다.

## 구현 순서

1. `books.image_url` 컬럼 존재 여부를 확인한다.
2. 컬럼이 없으면 schema 변경 필요성을 사용자에게 보고하고 승인받는다.
3. 외부 API 제공자를 Kakao Developers로 확정할지 확인한다.
4. API key와 환경 변수 저장 방식을 승인받는다.
5. backend 응답 DTO에 `imageUrl`을 추가한다.
6. 기존 API 응답에서 DB의 `image_url`이 내려오게 한다.
7. 이미지 URL 보강용 외부 API 호출 흐름을 구현한다.
8. 보강된 URL을 DB에 저장한다.
9. frontend 카드/상세 화면에서 `imageUrl`을 표시한다.
10. 이미지가 없거나 실패하면 기존 색상 표지로 fallback한다.
11. backend/frontend 검증 명령을 실행한다.

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

- `imageUrl`이 있는 책은 카드에서 실제 표지 이미지가 보인다.
- `imageUrl`이 있는 책은 상세 화면에서 실제 표지 이미지가 보인다.
- `imageUrl`이 없는 책은 기존 카테고리 색상 fallback이 보인다.
- 이미지 로드 실패 시 레이아웃이 깨지지 않는다.
- 홈, 랭킹, 책 상세, 마이페이지에서 표지 영역 비율이 유지된다.

## 제외 범위

이번 계획에서는 다음을 제외한다.

- 이미지 파일 자체 다운로드 및 서버 저장
- CDN 업로드
- 외부 API를 매 화면 요청마다 호출하는 방식
- 사용자 직접 이미지 업로드
- OCR 또는 이미지 품질 자동 검수
- 다중 이미지 후보를 사용자가 선택하는 UI

## 리스크와 후속 작업

- 외부 API key와 환경 변수 추가가 필요하므로 구현 전 승인이 필요하다.
- `books.image_url` 컬럼이 없으면 DB schema 변경 승인이 필요하다.
- API rate limit 때문에 일괄 보강은 재시도/중단 지점 관리가 필요할 수 있다.
- title/author 매칭이 부정확하면 잘못된 표지가 저장될 수 있다.
- 외부 이미지 URL이 나중에 만료되거나 접근 불가가 될 수 있다.
- 장기적으로는 이미지 URL 검증, 재보강, provider 변경 정책이 필요하다.
