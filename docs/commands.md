# Project Commands

이 문서는 ChaekList 작업 후 검증에 사용할 명령을 정리합니다.
명령은 Windows PowerShell 기준입니다.

## Frontend

위치: `frontend/`

### 개발 서버 실행

```powershell
cd frontend
npm run dev
```

### 프로덕션 빌드 검증

frontend 코드를 변경한 뒤 실행합니다.

```powershell
cd frontend
npm run build
```

### 빌드 결과 미리보기

```powershell
cd frontend
npm run start
```

## Backend

위치: `backend/`

### 환경 파일 초기화

backend 환경 파일을 초기화하거나 갱신할 때 실행합니다.

```powershell
cd backend
.\scripts\init-env.ps1
```

### 테스트 실행

backend 코드를 변경한 뒤 실행합니다.

```powershell
cd backend
.\gradlew.bat test
```

### 빌드 검증

```powershell
cd backend
.\gradlew.bat build
```

### Spring Boot 서버 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

## 권장 검증 기준

- frontend만 변경한 경우: `npm run build`
- backend만 변경한 경우: `.\gradlew.bat test`
- frontend와 backend를 모두 변경한 경우: 두 검증 명령을 모두 실행
- README.md, docs 문서만 변경한 경우: 별도 빌드 검증은 필요하지 않으며, 변경 내용과 링크를 확인

## 참고

- frontend 테스트 스크립트는 현재 `package.json`에 정의되어 있지 않습니다.
- backend는 Java 21 toolchain을 사용합니다.
