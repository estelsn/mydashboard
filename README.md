# AI FOMO Dashboard

AI 관련 정보를 여러 출처에서 수집하고, 중복을 제거한 뒤 중요도와 행동 필요성에 따라 분류하는 로컬 대시보드입니다. 현재 MVP의 주 수집 경로는 Threads 계정이며, 사용자가 직접 로그인한 Chrome 세션을 Playwright가 재사용합니다.

## 문제 정의

AI 도구와 기술 정보는 여러 계정과 채널에 흩어져 있어 다음 문제가 발생합니다.

- 같은 내용이 반복 노출되어 검토 시간이 늘어납니다.
- 지금 확인할 정보와 나중에 볼 정보를 빠르게 구분하기 어렵습니다.
- 수집 실패, 로그인 만료, 중복 제거 결과를 한곳에서 확인하기 어렵습니다.

이 프로젝트는 수집, 저장, 중복 제거, 규칙 기반 평가, 사용자 결정 상태 관리를 하나의 로컬 워크플로로 묶는 것을 목표로 합니다.

## 대상 사용자

- 여러 AI 정보 출처를 주기적으로 확인하는 개인 개발자
- 수집 결과를 `오늘 볼 것`, `나중에 볼 것`, `검토 필요`로 나누어 관리하려는 사용자
- 브라우저 자동화 수집 과정을 로컬에서 직접 통제하려는 사용자

## 현재 구현 상태

| 영역 | 상태 | 설명 |
| --- | --- | --- |
| Threads 수집 | 구현 및 UI 연동 | 단일 계정, 활성 계정 전체, 최근 3일 수집 |
| Threads 로그인 세션 | 구현 | 애플리케이션 전용 Chrome 프로필과 수동 로그인 사용 |
| 저장 및 정규화 | 구현 | 원문과 정규화된 정보 항목을 H2에 저장 |
| 중복 제거 | 구현 | URL 및 정규화된 콘텐츠의 SHA-256 해시 기준 |
| 규칙 기반 평가 | 구현 | 점수, 결정 상태, 한글 평가 이유 저장 |
| 수동 상태 변경 | 구현 | 오늘 볼 것, 나중에 볼 것, 검토 필요, 숨김 상태 관리 |
| 소스 관리 | 부분 구현 | 기존 소스 활성화/비활성화만 지원 |
| 수집 실행 이력 | 구현 | 실행 및 출처별 결과 조회, 실행 이력 삭제 |
| RSS 수집 | 내부 구현 | 수집 서비스와 테스트는 있으나 API, UI, 스케줄러 미연결 |
| LLM 평가 | STUB | 외부 모델 호출 없이 0점 평가 레코드만 생성 |
| 자동 스케줄링 | 미구현 | 수집은 사용자가 직접 실행 |
| 사용자 인증/다중 사용자 | 미구현 | 개인 로컬 사용을 전제로 함 |

### Evaluation Pipeline 구분

Evaluation Pipeline 전체가 STUB인 것은 아닙니다.

- `EvaluationService`의 평가 실행, 결과 저장, 재평가 API, 수동 결정 보호는 실제 구현입니다.
- `RuleBasedEvaluator`의 `rule-v3` 분류 로직은 실제 구현이며 테스트가 있습니다.
- `EvaluatorType.RULE_BASED_STUB`은 현재 동작과 맞지 않는 과거 명칭이지만, 해당 평가기는 실제 규칙 기반 구현입니다.
- `EvaluatorType.LLM_READY_STUB`은 실제 STUB입니다. 외부 LLM 연동과 프롬프트 기반 평가는 구현되어 있지 않습니다.

## 주요 기능

### 정보 수집

- Threads 계정 한 개 수동 수집
- 활성화된 Threads 출처 순차 수집
- 최근 3일 게시물 수집
- 계정별 성공, 실패, 로그인 필요 상태 기록
- 동일 계정의 중복 실행 및 짧은 시간 내 재수집 제한

### 저장 및 중복 제거

- 원본 수집 데이터와 정규화된 정보 항목 분리 저장
- 원본 URL 중복 확인
- 상대 날짜, 절대 날짜, URL을 제외한 안정화 콘텐츠 해시 비교
- 중복 후보는 새 정보 항목으로 저장하지 않음

### 평가 및 분류

- 관련성, 행동 가능성, 신규성 점수 계산
- 키워드, 카테고리, 본문 품질에 따른 규칙 기반 분류
- `APPLY`: 오늘 볼 것
- `HOLD`: 나중에 볼 것
- `UNREVIEWED`: 검토 필요
- `IGNORE`, `ARCHIVE_CANDIDATE`: 숨김 항목
- 한글 평가 이유 제공
- 사용자가 직접 변경한 결정은 자동 재평가에서 보호

### 조회 및 관리

- 대시보드 요약 수치
- 상태별 정보 카드 조회
- 숨김 항목 표시 전환
- 원문 링크 이동
- 출처 활성화/비활성화
- 최근 수집 실행 이력 조회와 삭제

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database
- Playwright Java 1.60.0
- Gradle Wrapper

### Frontend

- React 19
- Vite 7
- Tailwind CSS 3
- Vitest 3
- Testing Library

## 시스템 구조

```text
┌──────────────────────────────┐
│ React Dashboard              │
│ - 정보 분류/조회             │
│ - 수집 실행/이력             │
│ - 소스 및 세션 관리          │
└──────────────┬───────────────┘
               │ /api (Vite proxy)
┌──────────────▼───────────────┐
│ Spring Boot API              │
│ Controllers / Services       │
├──────────────┬───────────────┤
│ Collector    │ Evaluation    │
│ - Threads    │ - rule-v3     │
│ - RSS 내부형 │ - LLM STUB    │
├──────────────▼───────────────┤
│ Persistence / Spring Data JPA│
│ Source, CollectionRun,       │
│ CollectedItem, InfoItem,     │
│ Evaluation                   │
└──────────────┬───────────────┘
               │
┌──────────────▼───────────────┐
│ H2 File Database             │
│ backend/data/aifomo          │
└──────────────────────────────┘

Threads Collector
  └─ Playwright
      └─ 전용 Chrome 프로필
          └─ 사용자가 만든 Threads 로그인 세션
```

## 데이터 흐름

```text
Source
  → CollectionRun / CollectionSourceResult
  → CollectedItem(raw)
  → 중복 확인 및 정규화
  → InfoItem
  → Evaluation
  → Dashboard
```

`CollectedItem`은 수집 원문과 처리 상태를 보존하고, `InfoItem`은 화면에서 사용하는 제목, 요약, 카테고리, 결정 상태를 관리합니다. `Evaluation`은 평가 이력을 별도 엔티티로 저장합니다.

## 프로젝트 구조

```text
.
├── README.md
├── PORTFOLIO.md
├── backend/
│   ├── build.gradle
│   ├── data/                    # 로컬 H2 데이터, Git 제외
│   ├── runtime/                 # Chrome 프로필, Git 제외
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   └── resources/
│       └── test/
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
├── docs/
├── build.gradle
├── settings.gradle
└── gradlew
```

`docs/`에는 설계 및 개발 과정 문서가 포함되어 있습니다. 일부 문서는 초기 계획을 기록하므로 현재 구현 범위는 이 README와 실제 코드를 우선 기준으로 확인해야 합니다.

## 주요 API

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/dashboard/summary` | 상태별 요약 조회 |
| `GET` | `/api/info-items` | 정보 항목 조회 |
| `PATCH` | `/api/info-items/{id}/decision-status` | 사용자 결정 변경 |
| `PATCH` | `/api/info-items/{id}/archive` | 항목 숨김 |
| `PATCH` | `/api/info-items/{id}/restore` | 숨김 복원 |
| `GET` | `/api/sources` | 출처 목록 조회 |
| `PATCH` | `/api/sources/{id}/enabled` | 출처 활성 상태 변경 |
| `GET` | `/api/threads/session` | Threads 세션 상태 확인 |
| `POST` | `/api/threads/session/open-login` | 로그인용 Chrome 실행 |
| `POST` | `/api/collection-runs/threads` | 단일 Threads 출처 수집 |
| `POST` | `/api/collection-runs/threads/recent` | 최근 게시물 수집 |
| `POST` | `/api/collection-runs/threads/enabled` | 활성 출처 전체 수집 |
| `GET` | `/api/collection-runs` | 최근 수집 이력 조회 |
| `DELETE` | `/api/collection-runs/{id}` | 수집 이력 삭제 |
| `POST` | `/api/evaluations/info-items/{id}/recalculate` | 항목 재평가 |
| `POST` | `/api/evaluations/unreviewed/recalculate` | 미검토 항목 일괄 재평가 |

## 실행 방법

### 사전 요구사항

- JDK 21
- Node.js 20.19.x 또는 22.12 이상
- npm
- Google Chrome

기본 Chrome 경로는 macOS의 `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`입니다. 다른 운영체제나 설치 경로에서는 설정을 재정의해야 합니다.

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

Gradle 작업의 실행 디렉토리는 `backend/`이므로 실제 DB 파일은 `backend/data/`에 생성됩니다.

### Frontend

별도 터미널에서 실행합니다.

```bash
cd frontend
npm ci
npm run dev
```

대시보드 주소는 `http://localhost:5173`입니다. 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

## Threads 최초 설정

1. Backend와 Frontend를 실행합니다.
2. 대시보드에서 로그인 브라우저를 엽니다.
3. 열린 Chrome에서 Threads에 직접 로그인합니다.
4. 로그인이 완료되면 로그인 브라우저를 닫습니다.
5. 세션 상태를 다시 확인한 뒤 수집을 실행합니다.

애플리케이션은 계정 비밀번호나 쿠키 문자열을 직접 저장하지 않습니다. 로그인 상태는 `backend/runtime/browser-profiles/threads`의 전용 Chrome 프로필에 보존됩니다.

API로 로그인 브라우저를 열려면 다음 명령을 사용할 수 있습니다.

```bash
curl -X POST http://localhost:8080/api/threads/session/open-login
curl http://localhost:8080/api/threads/session
```

## 테스트 방법

### Backend

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:test
```

### Frontend

```bash
cd frontend
npm ci
npm test
```

## 빌드 방법

Backend:

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:build
```

Frontend:

```bash
cd frontend
npm ci
npm run build
```

Frontend 빌드는 `frontend/dist/`에 생성되며 Spring Boot 빌드에 자동 포함되지는 않습니다.

## 수동 검증

Backend 실행 후:

```bash
curl http://localhost:8080/api/dashboard/summary
curl http://localhost:8080/api/sources
curl http://localhost:8080/api/threads/session
curl http://localhost:8080/api/collection-runs
```

Frontend에서는 다음을 확인합니다.

1. 세 개의 기본 분류 영역과 요약 수치가 표시되는지 확인합니다.
2. 정보 항목의 상태를 변경하고 새로고침 후 유지되는지 확인합니다.
3. 출처 활성 상태를 변경하고 수집 대상에 반영되는지 확인합니다.
4. Threads 세션 확인 후 수집을 실행합니다.
5. 실행 카드를 펼쳐 출처별 성공 및 실패 이유를 확인합니다.
6. 새 항목의 평가 이유가 한글로 표시되는지 확인합니다.

## 설정

주요 설정은 `backend/src/main/resources/application.yml`에 있습니다.

- `app.threads.browser-session.chrome-executable`: Chrome 실행 파일
- `app.threads.browser-session.profile-directory`: 전용 Chrome user-data 디렉터리
- `app.threads.browser-session.profile-name`: user-data 디렉터리 내부의 Chrome 프로필 이름
- `app.threads.collection.safety.min-source-recollection-interval`: 출처별 재수집 제한
- `app.threads.collection.defaults.max-scroll-count`: 기본 스크롤 수
- `app.threads.collection.defaults.max-posts-per-account`: 기본 계정별 게시물 수
- `app.threads.collection.limits.max-scroll-count`: 요청 가능한 최대 스크롤 수
- `app.threads.collection.limits.max-posts-per-account`: 요청 가능한 최대 계정별 게시물 수
- `app.threads.browser-collector.headless`: 수집 브라우저 headless 실행 여부
- `app.threads.browser-collector.timeout`: 브라우저 수집 제한 시간

최근 수집 기간 3일은 현재 `ManualThreadsCollectionService`의 상수로 고정되어 있어 설정으로 변경할 수 없습니다.

예시:

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:bootRun \
  --args='--app.threads.browser-session.chrome-executable=/path/to/chrome'
```

## 트러블슈팅

### Threads 로그인이 필요하다고 표시됨

- 로그인 브라우저를 열어 실제 Threads 로그인을 완료합니다.
- 로그인 완료 후 Chrome을 닫아야 수집기가 같은 프로필을 사용할 수 있습니다.
- `GET /api/threads/session`으로 세션 상태를 다시 확인합니다.
- 로그인 페이지나 차단 페이지가 반환되면 수집은 중단되고 실행 이력에 이유가 기록됩니다.

### Chrome 프로필 잠금 오류

로그인용 Chrome과 수집기를 동시에 실행하면 같은 프로필을 열 수 없습니다. 전용 프로필을 사용 중인 Chrome 프로세스를 정상 종료한 후 다시 실행합니다. 활성 Chrome이 있는 상태에서 `SingletonLock` 파일을 임의로 삭제하지 마십시오.

### 브라우저 자동화가 Chrome을 찾지 못함

기본값은 macOS 경로입니다. 설치된 Chrome 실행 파일을 `app.threads.browser-session.chrome-executable`로 지정합니다. 서버 환경에는 브라우저와 필요한 런타임 의존성이 별도로 필요합니다.

### 최근 수집에서 오래된 데이터만 보이거나 새 항목이 없음

- 최근 수집은 기본적으로 게시 시각이 최근 3일 이내인 항목만 저장합니다.
- 게시 시각을 해석할 수 없는 항목은 최근 수집에서 제외됩니다.
- Threads의 고정 게시물이나 정렬 결과 때문에 오래된 글이 먼저 나타날 수 있습니다.
- 동일 출처는 기본 60분 재수집 제한이 적용됩니다.
- URL 또는 콘텐츠 해시가 같은 항목은 중복으로 판단되어 새 항목으로 저장되지 않습니다.
- 실행 카드를 펼쳐 `fetched`, `saved`, `duplicate`, `skipped` 수치와 출처별 메시지를 확인합니다.

### 특정 출처 실패가 전체 수집을 중단함

일반적인 계정 접근 실패와 타임아웃은 해당 출처 실패로 격리됩니다. 로그인 만료나 Chrome 프로필 잠금처럼 모든 출처에 영향을 주는 상태는 이후 출처 수집도 중단합니다.

### 수집 실행이 계속 진행 중으로 남음

비정상 종료 시 `RUNNING` 상태가 자동 복구되지 않을 수 있으며 새 실행을 차단할 수 있습니다. 최근 실행 ID를 확인한 뒤 불필요한 실행 이력을 삭제합니다.

```bash
curl http://localhost:8080/api/collection-runs
curl -X DELETE http://localhost:8080/api/collection-runs/{id}
```

### Frontend에서 API 연결 실패

- Backend가 `localhost:8080`에서 실행 중인지 확인합니다.
- Frontend 개발 서버는 `localhost:5173`에서 실행합니다.
- 현재 Backend CORS 설정은 `http://localhost:5173`만 허용합니다.
- Vite 프록시를 사용하지 않고 직접 API를 호출하면 API 주소와 CORS 설정을 함께 확인해야 합니다.

### 포트 충돌

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

기존 프로세스를 종료하거나 포트를 변경합니다. Frontend 포트를 변경하면 Backend CORS 설정도 일치시켜야 합니다.

### RSS 수집이 화면에서 실행되지 않음

RSS 파서, 수집 서비스, 저장 로직은 구현되어 있지만 현재 Controller, Frontend, Scheduler에 연결되어 있지 않습니다. RSS는 사용자 실행 가능한 MVP 기능이 아닙니다.

## 제한 사항

- Threads DOM과 서비스 정책 변경에 따라 수집기가 영향을 받을 수 있습니다.
- 개인 로컬 환경을 전제로 하며 사용자 인증과 권한 분리가 없습니다.
- 수집은 자동 스케줄링되지 않습니다.
- 출처 추가, URL 수정, 삭제는 UI에서 지원하지 않습니다.
- 규칙 기반 평가는 개인화 모델이나 의미 기반 분류를 제공하지 않습니다.
- LLM 평가기는 STUB 상태입니다.
- H2 파일 DB는 로컬 MVP에는 적합하지만 다중 인스턴스와 운영 배포에는 적합하지 않습니다.
- RSS 수집은 외부 인터페이스에 연결되지 않았습니다.

## 향후 개선 사항

1. `LLM_READY_STUB`을 실제 모델 연동 평가기로 대체
2. 평가 근거와 신뢰도에 대한 사용자 피드백 반영
3. RSS 수집 API, UI, 스케줄러 연결
4. 출처 생성, 수정, 삭제 기능 추가
5. 실패 실행 복구와 재시도 정책 구현
6. Threads DOM 변경 감지와 회귀 테스트 강화
7. PostgreSQL 등 운영용 DB 전환
8. 사용자 인증, 비밀정보 관리, 배포 설정 추가
9. 관측성, 구조화 로그, 메트릭 추가

## 포트폴리오

개발 과정, 기술적 의사결정, 역할, 면접용 설명은 [PORTFOLIO.md](./PORTFOLIO.md)에 정리되어 있습니다.
