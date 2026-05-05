# AI FOMO Dashboard

AI 관련 소스에서 수집한 항목을 중요도와 의사결정 상태 기준으로 정리하는 로컬 대시보드입니다.

## Requirements

- Java 21
- Node.js 20 이상
- npm

## Local Run

백엔드:

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:bootRun
```

프론트엔드:

```bash
cd frontend
npm install
npm run dev
```

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

## Verification

백엔드 테스트:

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:test
```

프론트엔드 테스트:

```bash
cd frontend
npm test
```

프론트엔드 빌드:

```bash
cd frontend
npm run build
```

가능하면 전체 Gradle 빌드도 실행합니다.

```bash
GRADLE_USER_HOME=.gradle ./gradlew build
```

## Threads Login Session

Threads 수집은 앱 전용 Chrome 프로필 디렉터리를 재사용하는 방식으로 로그인 세션을 다룹니다.

- 기본 프로필 경로: `./runtime/browser-profiles/threads`
- 로그인 브라우저 열기 API는 이 프로필 경로로 Chrome을 실행합니다.
- 사용자는 열린 Chrome에서 직접 Threads에 로그인합니다.
- 이후 Threads 수집기는 같은 로컬 브라우저 프로필을 사용해 세션 상태를 확인합니다.

`./runtime/browser-profiles/threads`는 로컬 실행 중 생성되는 브라우저 프로필 저장소입니다. Git 추적 대상이 아니며, 개발 머신 밖으로 공유하지 않는 런타임 데이터로 취급합니다.

앱은 다음 정보를 DB, API 응답, 문서, 테스트 픽스처에 저장하지 않습니다.

- Threads ID
- 비밀번호
- 인증코드
- 쿠키 문자열
- 세션 토큰

## Implemented Scope

- Spring Boot 백엔드 도메인, API, 서비스 계층
- H2 기반 로컬 저장소
- 정보 항목 조회 및 의사결정 상태 업데이트
- 룰 기반 평가 파이프라인
- CollectionRun 조회
- Source 조회 및 관리 API/UI
- Threads 세션 상태 API 및 로그인 브라우저 실행 API
- Threads HTML fixture/parser 기반 게시물 추출
- Threads 브라우저 수집 추상화와 수집 결과 저장
- 수동 Threads 수집 API/UI
- RSS/공식 소스 수집
- React/Vite 대시보드 UI

## Excluded Scope

- 실제 운영 계정 자동 로그인
- Threads ID, 비밀번호, 인증코드, 쿠키 문자열, 세션 토큰 저장
- GraphQL 직접 호출 기반 Threads 수집
- 쿠키 문자열 내보내기 또는 세션 토큰 추출
- RSS 수집 대상의 공식 발표 매칭 자동화
- LLM 연동
- Topic clustering
- Login/auth 사용자 계정 시스템
- 배포용 운영 인프라 구성
