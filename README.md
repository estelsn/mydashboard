# AI FOMO Dashboard

AI 관련 정보를 Threads 계정에서 수집하고, 중복 제거와 규칙 기반 평가를 거쳐 `오늘 볼 것`, `나중에 볼 것`, `검토 필요`로 정리하는 개인용 로컬 대시보드입니다.

현재 MVP의 주 수집 경로는 Threads입니다. 사용자가 직접 로그인한 Chrome 프로필을 Playwright가 재사용하며, 수집 결과는 로컬 H2 파일 DB에 저장됩니다.

## 1. 프로젝트 소개

AI 도구와 기술 정보는 여러 계정과 채널에 흩어져 있고, 같은 내용이 반복 노출됩니다. 이 프로젝트는 개인 개발자가 자주 확인하는 AI 정보를 로컬에서 수집하고, 중복과 우선순위를 정리해 검토 비용을 줄이는 것을 목표로 합니다.

현재는 제품 배포용 웹서비스가 아니라 로컬 단일 사용자 MVP입니다.

## 2. 해결하려는 문제

- 여러 Threads 계정을 반복 방문해야 합니다.
- 같은 게시물이 여러 번 보여 검토 시간이 늘어납니다.
- 수집 실패가 로그인, 접근 제한, 중복, 날짜 제외 중 어디서 발생했는지 확인하기 어렵습니다.
- 자동 평가 결과보다 사용자의 최종 판단이 우선되어야 합니다.

## 3. 주요 기능

- Threads 단일 출처 수집
- 활성화된 Threads 출처 전체 수집
- 최근 3일 게시물 수집
- 전용 Chrome 프로필 기반 Threads 로그인 세션 재사용
- 원문 URL과 정규화된 `contentHash` 기반 중복 제거
- 수집 실행과 출처별 결과 저장
- 규칙 기반 평가(`rule-v3`)와 한글 평가 근거 저장
- `오늘 볼 것`, `나중에 볼 것`, `검토 필요`, 숨김 상태 관리
- 사용자가 직접 변경한 결정 상태 보호
- 출처 활성화/비활성화
- 최근 수집 실행 이력 조회 및 삭제

## 4. 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.3.5, Spring Web, Spring Data JPA |
| Frontend | React 19, Vite 7, Tailwind CSS 3 |
| Browser Automation | Playwright Java 1.60.0, Google Chrome |
| Database | H2 file database |
| Test | JUnit 5, Spring Boot Test, Vitest 3, Testing Library, jsdom |
| Build | Gradle Wrapper, npm |

## 5. 시스템 구조 요약

```text
React Dashboard
  - 정보 카드 조회와 상태 변경
  - Threads 수집 실행
  - 세션 상태와 실행 이력 확인
        |
        | /api
        v
Spring Boot API
  - Collection Service
  - Threads Collector + Playwright
  - Persistence Service
  - Evaluation Service + RuleBasedEvaluator
        |
        v
H2 File Database
  - Source
  - CollectionRun / CollectionSourceResult
  - CollectedItem / InfoItem / Evaluation

Threads
  -> 전용 Chrome 프로필
  -> 사용자 수동 로그인 세션
  -> Playwright persistent context
```

```text
Source
  -> CollectionRun / CollectionSourceResult
  -> CollectedItem(raw)
  -> URL 및 contentHash 중복 확인
  -> InfoItem(normalized)
  -> Evaluation(rule-v3)
  -> Dashboard
```

## 6. 실행 방법

### 사전 요구사항

- JDK 21
- Node.js 20.19.x 또는 22.12 이상
- npm
- Google Chrome

기본 Chrome 경로는 macOS 기준입니다.

```text
/Applications/Google Chrome.app/Contents/MacOS/Google Chrome
```

다른 운영체제나 설치 경로에서는 `app.threads.browser-session.chrome-executable` 설정을 변경해야 합니다.

### Backend

프로젝트 루트에서 실행합니다.

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:bootRun
```

- API: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/aifomo`
- User: `sa`
- Password: 없음

Gradle 작업의 실행 디렉터리는 `backend/`이므로 실제 DB 파일은 `backend/data/`에 생성됩니다.

### Frontend

별도 터미널에서 실행합니다.

```bash
cd frontend
npm ci
npm run dev
```

- Dashboard: `http://localhost:5173`
- Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

### Threads 로그인 세션

1. Backend와 Frontend를 실행합니다.
2. 대시보드에서 로그인 브라우저를 엽니다.
3. 열린 Chrome에서 Threads에 직접 로그인합니다.
4. 로그인 완료 후 Chrome을 닫습니다.
5. 세션 상태를 다시 확인한 뒤 수집을 실행합니다.

API로도 로그인 브라우저를 열 수 있습니다.

```bash
curl -X POST http://localhost:8080/api/threads/session/open-login
curl http://localhost:8080/api/threads/session
```

애플리케이션은 계정 비밀번호나 쿠키 문자열을 DB에 저장하지 않습니다. 로그인 상태는 `backend/runtime/browser-profiles/threads`의 전용 Chrome 프로필에 보존됩니다.

## 7. 테스트 / 빌드 방법

### Backend 테스트

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:test
```

### Frontend 테스트

```bash
cd frontend
npm ci
npm test
```

### Backend 빌드

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:build
```

### Frontend 빌드

```bash
cd frontend
npm ci
npm run build
```

Frontend 빌드는 `frontend/dist/`에 생성되며 Spring Boot 빌드에 자동 포함되지는 않습니다.

## 8. 현재 구현 상태

### 구현 완료

| 영역 | 상태 |
| --- | --- |
| Threads 수집 | 단일 출처, 활성 출처 전체, 최근 3일 수집 구현 |
| Threads 로그인 세션 | 전용 Chrome 프로필과 사용자 수동 로그인 기반 구현 |
| 저장 및 정규화 | 원문, 정규화 항목, 평가 이력 분리 저장 |
| 중복 제거 | 원문 URL과 정규화된 `contentHash` 기준 구현 |
| 평가 | `rule-v3` 규칙 기반 평가 구현 |
| 사용자 결정 | 수동 결정 변경과 자동 재평가 보호 구현 |
| 실행 이력 | 전체 실행과 출처별 결과 조회 구현 |
| 소스 관리 | 기존 출처 활성화/비활성화 구현 |

### 제한 사항

- `LLM_READY_STUB`은 실제 STUB입니다. 외부 LLM 호출과 프롬프트 기반 평가는 구현되어 있지 않습니다.
- RSS 파서와 내부 수집 서비스는 있으나 Controller, Frontend, Scheduler에 연결되어 있지 않습니다. 현재 사용자 기능이 아닙니다.
- 자동 스케줄링은 없습니다. 수집은 사용자가 직접 실행합니다.
- 출처 생성, URL 수정, 삭제 UI는 없습니다.
- 사용자 인증과 다중 사용자 권한 분리는 없습니다.
- 비정상 종료된 `RUNNING` 실행의 자동 복구 기능은 없습니다.
- Threads DOM, 로그인 정책, Chrome 프로필 상태 변화의 영향을 받을 수 있습니다.
- H2 파일 DB는 로컬 MVP용이며 운영 배포용 DB로 보기는 어렵습니다.

### 향후 개선

- 실제 LLM 평가기 연동과 실패/비용 정책 추가
- RSS 수집 API, UI, 스케줄러 연결
- 출처 생성, 수정, 삭제 기능 추가
- 실패 실행 복구와 재시도 정책 구현
- Threads DOM 변경 감지와 회귀 테스트 강화
- PostgreSQL 등 운영용 DB 전환
- 사용자 인증, 비밀정보 관리, 배포 설정 추가

## 9. 대표 트러블슈팅 요약

### Threads 세션 READY 안정화

로그인 후에도 수집기가 `LOGIN_REQUIRED`로 판단하는 문제가 있었습니다. 로그인 브라우저와 수집 브라우저가 같은 고정 Chrome 프로필을 사용하도록 통합하고, 디렉터리 존재 여부가 아니라 실제 Threads 페이지의 인증 쿠키와 로그인 UI를 함께 검사하도록 변경했습니다.

### 최근 게시물 판정 및 중복 제거

최근 수집에서 오래된 고정 게시물이나 날짜 불명 게시물이 최신 데이터처럼 처리될 수 있었습니다. ISO, 영문·한글 상대 시각, 월·일 형식을 해석하고, 날짜를 알 수 없는 항목은 최근 수집에서 제외했습니다. 중복 제거는 URL과 날짜 표현을 제거한 본문 기반 `contentHash`와 원문 URL을 함께 사용합니다.

### 다중 출처 `PARTIAL_SUCCESS` 처리

여러 출처 중 일부 계정만 실패해도 전체 실패처럼 보이는 문제가 있었습니다. `CollectionRun`과 `CollectionSourceResult`를 분리하고 `PARTIAL_SUCCESS` 상태를 도입해 출처별 성공, 실패, 생성, 중복, 실패 메시지를 확인할 수 있도록 했습니다.

## 10. 스크린샷

이미지 파일은 아직 포함되어 있지 않습니다. 추가 예정 경로:

```text
docs/images/dashboard.png
docs/images/collection-runs.png
docs/images/threads-session.png
```
