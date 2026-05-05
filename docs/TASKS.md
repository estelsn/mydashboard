# AI FOMO Dashboard Tasks

## 작업 운영 원칙

이 문서는 Codex가 실제로 따라야 할 작업 순서다.

Codex는 PRD.md와 ARCHITECTURE.md를 전체 방향으로 참고하되, TASKS.md에서 열려 있는 Step만 구현한다.

LOCKED로 표시된 Step은 구현하지 않는다.

실제 Threads 수집, Chrome headless 실행, RSS, LLM, 공식 발표 자동 검증은 아직 구현하지 않는다.

## Step 1. Backend Domain Skeleton

Status: DONE

### 목표

Spring Boot 백엔드의 도메인 골격을 만든다.

### 구현 범위

- backend 모듈 생성 또는 확인
- Java 21
- Spring Boot 3.x
- Gradle
- base package: com.aifomo.dashboard
- H2 설정
- Entity 생성
  - Source
  - CollectedItem
  - InfoItem
  - Evaluation
- Enum 생성
  - SourceType
  - SourceCategory
  - CollectedItemStatus
  - DecisionStatus
  - ImportanceLevel
  - EvaluatorType
- Repository 생성
- DataSeeder 생성
  - CommandLineRunner 사용
  - 중복 삽입 방지
- contentHash 생성 유틸 구현
  - SHA-256(normalized rawContent)

### Step 1에서 만들지 말 것

- Controller
- Service API 로직
- React frontend
- Threads crawler
- Chrome 실행 코드
- Playwright/Puppeteer
- RSS crawler
- LLM evaluator
- official source matching
- topic clustering
- login/auth
- manual InfoItem input form
- physical delete

### DataSeeder 기준

초기 seed 데이터:

- Threads Source 9개
- 공식 발표 Source 3개
- CollectedItem 12개
- InfoItem 12개
- Evaluation 12개

관계:

- CollectedItem 12개와 InfoItem 12개는 1:1로 연결한다.
- InfoItem.collectedItem은 null이면 안 된다.
- Evaluation 12개는 각각의 InfoItem에 연결한다.
- seed Evaluation은 evaluatorType=SEED_SAMPLE을 사용한다.

### Source seed

Threads:

1. Choi OpenAI
   - sourceType: THREADS
   - category: NEWS
   - url: https://www.threads.com/@choi.openai

2. UncleJobs AI
   - sourceType: THREADS
   - category: NEWS
   - url: https://www.threads.com/@unclejobs.ai

3. Appcast
   - sourceType: THREADS
   - category: CODEX
   - url: https://www.threads.com/@appcast

4. Ethan CL
   - sourceType: THREADS
   - category: CODEX
   - url: https://www.threads.com/@ethancl

5. GPTaku AI
   - sourceType: THREADS
   - category: CLAUDE
   - url: https://www.threads.com/@gptaku_ai

6. Roach Log
   - sourceType: THREADS
   - category: HERMES
   - url: https://www.threads.com/@roach_log

7. Specal1849
   - sourceType: THREADS
   - category: IMAGE
   - url: https://www.threads.com/@specal1849

8. Xazinga
   - sourceType: THREADS
   - category: VIDEO
   - url: https://www.threads.com/@xazinga

9. Apple Tea 94
   - sourceType: THREADS
   - category: VIDEO
   - url: https://www.threads.com/@apple_tea_94

Official/Future sources:

10. OpenAI Official Blog
    - sourceType: OFFICIAL_BLOG
    - category: COMPANY_OFFICIAL

11. Anthropic News
    - sourceType: OFFICIAL_BLOG
    - category: COMPANY_OFFICIAL

12. Google AI Blog
    - sourceType: OFFICIAL_BLOG
    - category: COMPANY_OFFICIAL

### 완료 기준

- backend 앱이 실행된다.
- http://localhost:8080/h2-console 접속 가능하다.
- JDBC URL jdbc:h2:file:./data/aifomo 로 DB 확인 가능하다.
- Source 테이블에 12개 Source가 있다.
- CollectedItem 12개가 있다.
- InfoItem 12개가 있다.
- InfoItem은 CollectedItem과 연결되어 있다.
- Evaluation 12개가 있다.
- seed Evaluation의 evaluatorType은 SEED_SAMPLE이다.
- 앱을 재실행해도 seed 데이터가 중복 삽입되지 않는다.

## Step 2. Backend API

Status: DONE

Step 1 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- Source 목록 조회 API
- InfoItem 목록 조회 API
- InfoItem 상세 조회 API
- decisionStatus 수동 변경 API
- manualOverride 처리
- archive/restore API
- Dashboard summary API
- CORS 설정
  - http://localhost:5173 허용

### 금지

- 실제 Threads 수집 구현 금지
- LLM 호출 금지
- RSS 구현 금지

## Step 3. RuleBasedEvaluator

Status: DONE

Step 2 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- RuleBasedEvaluator 구현
- evaluatorType=RULE_BASED
- evaluatorVersion=rule-v1
- 미검토 항목 일괄 평가
- 개별 항목 평가
- manualOverride=true 항목은 decisionStatus 덮어쓰기 금지

## Step 4. Frontend Dashboard Skeleton

Status: DONE

Step 3 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- frontend 모듈 생성
- React + Vite + Tailwind CSS
- useState/useEffect 기반 구현
- 별도 상태관리 라이브러리 금지
- 별도 UI 컴포넌트 라이브러리 금지
- 기본 레이아웃
- 상단 요약 카드
- 오늘 볼 것 / 나중에 볼 것 / 검토 필요 3섹션
- 숨김 항목 보기 토글

## Step 5. Frontend Interaction

Status: DONE

Step 4 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- 상태 변경 버튼
- manualOverride 반영
- 숨김/복구 버튼
- 평가 점수 표시
- 평가 이유 표시

## Step 6. CollectionRun Skeleton

Status: DONE

Step 5 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- CollectionRun 엔티티 설계
- 수집 상태 표시용 구조만 구현
- 실제 수집기는 구현하지 않음

## Step 7. Threads Collector Design Only

Status: DONE

Step 6 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- Threads collector interface 설계
- THREADS_EXTRACTION_NOTES.md 기반 구현 계획 작성
- 실제 Chrome 실행 코드 작성 금지
- 실제 Threads 접속 코드 작성 금지

## Step 8. Threads Browser Session Strategy

Status: DONE

Goal:
Threads 수집에 사용할 로그인 세션 전략을 확정하고, 앱 전용 브라우저 프로필 기반 구조를 준비한다.

Context:
- Threads 정보 추출은 이미 별도 검증에서 성공한 상태다.
- 이후 단계의 목표는 검증된 Threads 수집 방식을 제품 코드에 안전하게 통합하는 것이다.
- 로그인 세션이 없으면 Threads에서 일부 게시글만 보이거나 접근이 제한될 수 있다.
- 앱은 Threads ID, 비밀번호, 인증코드, 쿠키 문자열, 세션 토큰을 직접 저장하지 않는다.
- 대신 앱 전용 Chrome profile directory를 사용한다.
- 기본 profile directory는 `./runtime/browser-profiles/threads` 로 둔다.
- 사용자는 해당 프로필로 열린 Chrome에서 직접 Threads에 로그인한다.
- 수집기는 같은 profile directory를 재사용해 로그인된 세션으로 Threads 페이지에 접근한다.
- `runtime/` 경로는 Git 추적 대상에서 제외되어야 한다.

Scope:
- Threads 브라우저 세션 전략을 문서화한다.
- 앱 전용 Chrome profile directory 사용 방침을 코드/설정 경계에 반영한다.
- 브라우저 세션 상태 enum을 정의한다.
- BrowserSessionProvider 또는 동등한 인터페이스를 추가한다.
- Threads 세션 상태를 표현할 DTO/서비스 구조를 준비한다.
- 실제 Threads 접속, Chrome 실행, 게시글 수집은 아직 구현하지 않는다.
- 테스트는 순수 단위 테스트로 작성한다.

Expected design:
- Browser session status values:
  - NOT_CONFIGURED
  - LOGIN_REQUIRED
  - READY
  - EXPIRED
  - ERROR
- Default profile directory:
  - `./runtime/browser-profiles/threads`
- No secrets in source code, docs, test fixtures, or config files.

Do not:
- Threads ID/PW 입력 기능을 만들지 마라.
- 인증코드 처리 기능을 만들지 마라.
- 쿠키 문자열이나 세션 토큰을 DB, yml, env, 문서, 테스트 fixture에 저장하지 마라.
- 실제 Chrome 실행을 구현하지 마라.
- 실제 Threads 접속을 구현하지 마라.
- Playwright/Puppeteer 기반 수집을 아직 구현하지 마라.
- 게시글 저장 로직을 구현하지 마라.
- Step 9 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- 브라우저 세션 상태 enum/DTO/서비스 단위 테스트가 통과해야 한다.
- `./runtime/browser-profiles/threads` 경로가 Git에 추적되지 않는 구조임을 유지해야 한다.
- docs/TASKS.md에서 Step 8 DONE, Step 9 OPEN 상태로 갱신한다.


## Step 9. Threads Session Status API

Status: OPEN

Goal:
프론트엔드와 수집 실행 흐름에서 Threads 로그인 세션 준비 상태를 확인할 수 있는 API를 만든다.

Context:
- Step 8에서 정의한 앱 전용 Chrome profile directory 전략을 사용한다.
- 이 단계에서는 실제 Threads 접속을 통한 로그인 검증을 하지 않는다.
- 세션 상태 판정은 우선 profile directory 존재 여부, 설정값, stub/checker 구조로 제한한다.
- 실제 Threads 페이지 접근 기반 검증은 이후 단계에서 구현한다.

Scope:
- `GET /api/threads/session` 또는 동등한 API를 추가한다.
- 현재 Threads session status를 반환하는 컨트롤러/서비스/DTO를 추가한다.
- 응답에는 status, profilePath, message를 포함한다.
- profile directory가 없으면 LOGIN_REQUIRED 또는 NOT_CONFIGURED 상태를 반환한다.
- 추후 실제 세션 검증 로직으로 교체 가능한 구조로 만든다.
- API 테스트를 추가한다.

Example response:
```json
{
  "status": "LOGIN_REQUIRED",
  "profilePath": "./runtime/browser-profiles/threads",
  "message": "Threads login is required before collection."
}
```

Do not:
- 실제 Chrome을 실행하지 마라.
- 실제 Threads에 접속하지 마라.
- 쿠키/토큰을 읽거나 출력하지 마라.
- 로그인 자동화를 구현하지 마라.
- 게시글 수집을 구현하지 마라.
- Step 10 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- session status API 테스트가 통과해야 한다.
- 응답에 민감정보가 포함되지 않아야 한다.
- docs/TASKS.md에서 Step 9 DONE, Step 10 OPEN 상태로 갱신한다.


## Step 10. Threads Login Browser Launcher

Status: LOCKED

Goal:
사용자가 앱 전용 Chrome profile directory로 열린 브라우저에서 직접 Threads에 로그인할 수 있게 한다.

Context:
- 앱은 로그인 정보를 입력받거나 저장하지 않는다.
- 사용자가 브라우저에서 직접 로그인한다.
- 앱은 Chrome을 앱 전용 profile directory로 여는 역할만 수행한다.
- 기본 profile directory는 `./runtime/browser-profiles/threads` 이다.

Scope:
- `POST /api/threads/session/open-login` 또는 동등한 API를 추가한다.
- macOS 기준 Chrome 실행 명령을 구성한다.
- Chrome 실행 경로와 profile directory는 설정값으로 분리한다.
- 기본 Threads URL은 `https://www.threads.net/` 로 둔다.
- Chrome 실행 실패 시 명확한 에러 응답을 반환한다.
- 런처 서비스 단위 테스트를 추가한다.
- 실제 Chrome 실행이 어려운 테스트 환경에서는 command builder를 분리해 테스트한다.

Expected behavior:
- API 호출 시 앱 전용 profile directory를 사용하는 Chrome 창을 연다.
- 사용자는 열린 Chrome에서 직접 Threads에 로그인한다.
- 이후 같은 profile directory를 재사용하면 로그인 상태가 유지된다.

Do not:
- Threads ID/PW 입력 UI/API를 만들지 마라.
- 인증코드 처리 기능을 만들지 마라.
- 쿠키/토큰을 코드로 추출하지 마라.
- 기본 사용자의 Chrome profile을 직접 재사용하지 마라.
- 게시글 수집을 구현하지 마라.
- Step 11 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- Chrome command builder 테스트가 통과해야 한다.
- profile directory가 `./runtime/browser-profiles/threads` 를 기본값으로 사용해야 한다.
- 응답/로그에 민감정보가 포함되지 않아야 한다.
- docs/TASKS.md에서 Step 10 DONE, Step 11 OPEN 상태로 갱신한다.


## Step 11. Threads Fixture Parser

Status: LOCKED

Goal:
실제 브라우저 수집기를 붙이기 전에, Threads HTML/DOM/JSON snapshot에서 게시글 정보를 안정적으로 파싱하는 로직을 만든다.

Context:
- 실제 Threads 페이지 구조는 변경될 수 있으므로 파싱 로직은 수집 로직과 분리한다.
- 테스트는 외부 Threads에 접속하지 않고 fixture 기반으로 수행한다.
- fixture에는 실제 계정 세션, 쿠키, 토큰, 개인정보를 포함하지 않는다.

Scope:
- ThreadsPostParser 또는 동등한 파서 클래스를 추가한다.
- fixture 기반 테스트 자료를 추가한다.
- 파싱 대상:
  - 작성자/계정 식별자
  - 게시글 본문
  - 게시글 URL
  - 작성 시각 또는 표시 시각
  - 원본 rawContent
- 빈 결과, 깨진 fixture, 일부 필드 누락 케이스를 테스트한다.
- contentHash 생성을 위한 normalized rawContent 기준을 연결한다.

Do not:
- 실제 Threads에 접속하지 마라.
- Chrome 실행을 구현하지 마라.
- Playwright/Puppeteer를 사용하지 마라.
- 쿠키/토큰/세션 파일을 fixture에 넣지 마라.
- DB 저장 로직을 구현하지 마라.
- Step 12 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- fixture parser 테스트가 통과해야 한다.
- fixture에 민감정보가 포함되지 않아야 한다.
- 파서가 빈 결과/누락 필드 케이스를 안전하게 처리해야 한다.
- docs/TASKS.md에서 Step 11 DONE, Step 12 OPEN 상태로 갱신한다.


## Step 12. Threads Browser Collector

Status: LOCKED

Goal:
앱 전용 Chrome profile directory를 재사용해 로그인된 Threads 페이지에서 게시글 원문 데이터를 가져오는 수집기를 구현한다.

Context:
- 사용자는 Step 10의 login browser launcher를 통해 직접 Threads에 로그인한다.
- 수집기는 같은 profile directory를 사용해 로그인된 세션으로 Threads 계정 페이지에 접근한다.
- 초기 구현은 headless=false를 기본으로 한다.
- 수집기는 제한된 횟수의 스크롤과 최대 게시글 수 제한을 반드시 둔다.

Scope:
- ThreadsBrowserCollector 또는 동등한 구현체를 추가한다.
- 앱 전용 profile directory를 사용하는 browser context를 구성한다.
- 수집 대상 Threads 계정 URL에 접근한다.
- maxScrollCount, maxPostsPerAccount 설정을 둔다.
- raw HTML, DOM snapshot, 또는 수집 가능한 원문 데이터를 확보한다.
- 확보한 rawContent를 Step 11의 parser에 전달한다.
- 로그인 필요/접근 제한/빈 결과/타임아웃을 명확한 실패 상태로 반환한다.
- 테스트는 외부 접속 없이 adapter/stub/fixture 기반으로 작성한다.

Configuration example:
```yaml
threads:
  browser:
    profile-dir: ./runtime/browser-profiles/threads
    headless: false
    max-scroll-count: 5
    max-posts-per-account: 20
```

Do not:
- ID/PW 자동 로그인을 구현하지 마라.
- 인증코드 자동 처리를 구현하지 마라.
- 쿠키 문자열이나 세션 토큰을 직접 저장하지 마라.
- 무제한 스크롤을 구현하지 마라.
- 무제한 계정 수집을 구현하지 마라.
- 테스트에서 실제 Threads에 접속하지 마라.
- DB 저장 로직은 최소화하고, 본격 저장 연동은 Step 13에서 처리한다.
- Step 13 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- collector adapter/stub 테스트가 통과해야 한다.
- 수집 제한값이 적용되어야 한다.
- 로그인 필요/빈 결과/실패 상태가 명확히 구분되어야 한다.
- docs/TASKS.md에서 Step 12 DONE, Step 13 OPEN 상태로 갱신한다.


## Step 13. Threads Collection Persistence

Status: LOCKED

Goal:
Threads 수집 결과를 CollectedItem, InfoItem, CollectionRun 흐름에 연결해 DB에 저장한다.

Context:
- Step 12에서 수집된 ThreadsCollectedPost를 제품 데이터로 변환한다.
- 중복 방지는 contentHash 기준으로 처리한다.
- CollectionRun에는 실행 시작/성공/실패 상태와 수집 개수를 기록한다.

Scope:
- ThreadsCollectedPost → CollectedItem 변환 로직을 추가한다.
- normalized rawContent 기반 contentHash를 생성한다.
- 기존 contentHash가 있으면 중복으로 처리하고 새로 저장하지 않는다.
- 신규 CollectedItem 저장 후 InfoItem과 연결한다.
- CollectionRun에 collectedCount, createdCount, duplicateCount, failedCount, failureReason 등을 기록한다.
- 실패해도 앱 전체가 죽지 않도록 실패 사유를 CollectionRun에 남긴다.
- 서비스 테스트를 추가한다.

Expected flow:
```text
ThreadsCollectedPost
  -> rawContent normalization
  -> contentHash
  -> duplicate check
  -> CollectedItem save
  -> InfoItem create/link
  -> CollectionRun result update
```

Do not:
- 프론트엔드 수동 실행 UI를 구현하지 마라.
- Source 관리 모델을 구현하지 마라.
- RSS 수집을 구현하지 마라.
- LLM 평가를 구현하지 마라.
- Step 14 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- 신규 저장/중복 처리/실패 처리 테스트가 통과해야 한다.
- CollectionRun 상태와 count가 정확히 기록되어야 한다.
- docs/TASKS.md에서 Step 13 DONE, Step 14 OPEN 상태로 갱신한다.


## Step 14. Manual Threads Collection API

Status: LOCKED

Goal:
사용자가 수동으로 Threads 계정 수집을 실행할 수 있는 API를 만든다.

Context:
- Step 12의 Threads collector와 Step 13의 persistence 흐름을 연결한다.
- 수집 실행은 CollectionRun으로 기록한다.
- 동시에 여러 수집이 중복 실행되지 않도록 최소한의 방어 로직을 둔다.

Scope:
- `POST /api/collection-runs/threads` 또는 동등한 API를 추가한다.
- 요청값:
  - accountUrls
  - maxPostsPerAccount
  - maxScrollCount
- 실행 중인 CollectionRun이 있으면 중복 실행을 거부하거나 명확한 에러를 반환한다.
- 실행 결과:
  - runId
  - status
  - collectedCount
  - createdCount
  - duplicateCount
  - failedCount
  - failureReason
- API 테스트를 추가한다.

Example request:
```json
{
  "accountUrls": [
    "https://www.threads.net/@example"
  ],
  "maxPostsPerAccount": 20,
  "maxScrollCount": 5
}
```

Example response:
```json
{
  "runId": 12,
  "status": "SUCCESS",
  "collectedCount": 18,
  "createdCount": 7,
  "duplicateCount": 11,
  "failedCount": 0
}
```

Do not:
- 프론트엔드 버튼을 구현하지 마라.
- Source 모델을 구현하지 마라.
- RSS 수집을 구현하지 마라.
- LLM 평가를 구현하지 마라.
- Step 15 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- 수동 실행 API 테스트가 통과해야 한다.
- 중복 실행 방어 테스트가 통과해야 한다.
- 실패 시 CollectionRun에 failureReason이 기록되어야 한다.
- docs/TASKS.md에서 Step 14 DONE, Step 15 OPEN 상태로 갱신한다.


## Step 15. Frontend Threads Session Panel

Status: LOCKED

Goal:
프론트엔드에서 Threads 로그인 세션 상태를 확인하고, 로그인 브라우저를 열 수 있게 한다.

Context:
- Step 9의 session status API와 Step 10의 login browser launcher API를 사용한다.
- 사용자는 앱 화면에서 현재 Threads 세션이 준비되었는지 확인할 수 있어야 한다.

Scope:
- Threads session panel 컴포넌트를 추가한다.
- 로그인 상태를 표시한다:
  - NOT_CONFIGURED
  - LOGIN_REQUIRED
  - READY
  - EXPIRED
  - ERROR
- `로그인 브라우저 열기` 버튼을 추가한다.
- `세션 상태 다시 확인` 버튼을 추가한다.
- API 호출 중 로딩 상태를 표시한다.
- 실패 시 에러 메시지를 표시한다.
- 프론트엔드 테스트를 추가한다.

Do not:
- 수집 실행 버튼을 구현하지 마라.
- Source 관리 UI를 구현하지 마라.
- RSS UI를 구현하지 마라.
- Step 16 이후 작업을 구현하지 마라.

Verification:
- `npm test`
- `npm run build`
- 로그인 상태별 UI 테스트가 통과해야 한다.
- docs/TASKS.md에서 Step 15 DONE, Step 16 OPEN 상태로 갱신한다.


## Step 16. Frontend Manual Threads Collection Trigger

Status: LOCKED

Goal:
프론트엔드에서 Threads 수집을 수동 실행하고 결과를 확인할 수 있게 한다.

Context:
- Step 14의 manual collection API를 사용한다.
- 수집 실행 후 CollectionRun 목록과 대시보드 요약을 갱신한다.

Scope:
- 수집 대상 Threads 계정 URL 입력 UI를 추가한다.
- maxPostsPerAccount, maxScrollCount 입력 또는 기본값을 제공한다.
- `수집 실행` 버튼을 추가한다.
- 실행 중 로딩 상태를 표시한다.
- 실행 결과 count를 표시한다.
- 실패 시 failureReason을 표시한다.
- 실행 후 CollectionRun 목록과 dashboard summary를 refresh한다.
- 프론트엔드 테스트를 추가한다.

Do not:
- Source 모델을 구현하지 마라.
- RSS 수집을 구현하지 마라.
- LLM 평가를 구현하지 마라.
- Step 17 이후 작업을 구현하지 마라.

Verification:
- `npm test`
- `npm run build`
- 수집 실행/성공/실패 UI 테스트가 통과해야 한다.
- docs/TASKS.md에서 Step 16 DONE, Step 17 OPEN 상태로 갱신한다.


## Step 17. Source Model

Status: LOCKED

Goal:
Threads 계정, 공식 블로그, RSS 피드를 공통 Source 개념으로 관리할 수 있는 모델을 추가한다.

Context:
- 초기에는 수집 대상 URL을 직접 입력할 수 있지만, 완성본에서는 주요 정보원을 저장하고 관리해야 한다.
- Source 모델은 Threads, 공식 블로그, RSS를 모두 포괄한다.

Scope:
- Source 또는 FeedSource 엔티티를 추가한다.
- SourceType enum을 추가한다:
  - THREADS_ACCOUNT
  - OFFICIAL_BLOG
  - RSS_FEED
- 필드:
  - id
  - name
  - sourceType
  - url
  - enabled
  - priority
  - lastCollectedAt
  - createdAt
  - updatedAt
- Repository, Service, Controller를 추가한다.
- 조회 API를 추가한다.
- seed sample을 추가한다.
- 테스트를 추가한다.

Do not:
- 실제 RSS fetch를 구현하지 마라.
- Source 관리 프론트엔드를 구현하지 마라.
- LLM 평가를 구현하지 마라.
- Step 18 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- Source 조회 API 테스트가 통과해야 한다.
- seed sample이 중복 생성되지 않아야 한다.
- docs/TASKS.md에서 Step 17 DONE, Step 18 OPEN 상태로 갱신한다.


## Step 18. Source Management UI

Status: LOCKED

Goal:
프론트엔드에서 수집 대상 Source 목록을 확인하고 기본 상태를 관리할 수 있게 한다.

Context:
- Step 17의 Source API를 사용한다.
- 초기 완성본에서는 Source 목록 확인과 enabled 상태 표시를 우선한다.

Scope:
- Source 목록 UI를 추가한다.
- 표시 항목:
  - name
  - sourceType
  - url
  - enabled
  - priority
  - lastCollectedAt
- enabled 상태를 배지 또는 토글 형태로 표시한다.
- 가능하면 enabled 토글 API까지 구현한다.
- 프론트엔드 테스트를 추가한다.

Do not:
- 복잡한 Source 편집 기능을 구현하지 마라.
- RSS 실제 수집을 구현하지 마라.
- LLM 평가를 구현하지 마라.
- Step 19 이후 작업을 구현하지 마라.

Verification:
- `npm test`
- `npm run build`
- Source 목록 UI 테스트가 통과해야 한다.
- docs/TASKS.md에서 Step 18 DONE, Step 19 OPEN 상태로 갱신한다.


## Step 19. RSS / Official Source Collector

Status: LOCKED

Goal:
공식 블로그와 RSS 피드 기반 수집 흐름을 추가한다.

Context:
- Threads 수집 흐름이 안정화된 뒤 공식 발표/RSS 수집을 추가한다.
- Source 모델을 사용해 enabled 상태인 RSS_FEED 또는 OFFICIAL_BLOG 소스를 대상으로 한다.
- CollectionRun, CollectedItem, InfoItem, contentHash 중복 방지 흐름은 Threads와 동일하게 재사용한다.

Scope:
- RSS collector 인터페이스/서비스를 추가한다.
- RSS item을 CollectedItem/InfoItem으로 변환한다.
- Source 기반 수집 대상을 조회한다.
- contentHash 중복 방지를 적용한다.
- CollectionRun과 연결한다.
- 테스트는 우선 fixture RSS XML 기반으로 작성한다.
- 실제 외부 네트워크 호출은 설정 또는 adapter로 분리한다.

Do not:
- LLM 평가를 구현하지 마라.
- 복잡한 RSS 스케줄러를 구현하지 마라.
- Step 20 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- RSS fixture parser/collector 테스트가 통과해야 한다.
- 중복 저장이 방지되어야 한다.
- CollectionRun count가 정확히 기록되어야 한다.
- docs/TASKS.md에서 Step 19 DONE, Step 20 OPEN 상태로 갱신한다.


## Step 20. Evaluation Pipeline

Status: LOCKED

Goal:
수집된 정보를 평가하는 파이프라인을 정리하고, 향후 LLM 평가 연동이 가능한 구조를 만든다.

Context:
- 현재 단계에서는 실제 LLM API를 호출하지 않는다.
- seed/sample 평가, 수동 평가, rule-based stub 평가를 구분한다.
- 평가 결과는 대시보드에서 점수/이유로 표시될 수 있어야 한다.

Scope:
- EvaluationService 구조를 정리한다.
- evaluatorType enum을 정리한다:
  - SEED_SAMPLE
  - MANUAL
  - RULE_BASED_STUB
  - LLM_READY_STUB
- 평가 재계산 서비스 또는 API를 추가한다.
- InfoItem과 Evaluation 연결 흐름을 정리한다.
- 테스트를 추가한다.

Do not:
- OpenAI/Claude/Gemini 등 실제 LLM API를 호출하지 마라.
- 외부 네트워크 호출을 구현하지 마라.
- Step 21 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- 평가 생성/갱신/재계산 테스트가 통과해야 한다.
- 기존 seed Evaluation과 새 평가 흐름이 충돌하지 않아야 한다.
- docs/TASKS.md에서 Step 20 DONE, Step 21 OPEN 상태로 갱신한다.


## Step 21. Dashboard Polish

Status: LOCKED

Goal:
현재까지 구현된 백엔드/프론트엔드 기능을 실제 사용할 수 있는 로컬 대시보드 형태로 정리한다.

Scope:
- 로딩 상태 UI를 정리한다.
- 빈 상태 UI를 정리한다.
- 에러 상태 UI를 정리한다.
- 날짜/시간 표시를 정리한다.
- 상태 배지 표현을 통일한다.
- CollectionRun 실패 메시지 표시를 정리한다.
- manualOverride, 평가 점수, 평가 이유 표시를 정리한다.
- 대시보드 카드/섹션 레이아웃을 정리한다.
- README 또는 docs에 실행 방법을 보강한다.

Do not:
- 신규 대형 기능을 추가하지 마라.
- 실제 LLM 연동을 추가하지 마라.
- 인증/회원 기능을 추가하지 마라.
- Step 22 이후 작업을 구현하지 마라.

Verification:
- `GRADLE_USER_HOME=.gradle ./gradlew :backend:test`
- `npm test`
- `npm run build`
- 주요 화면이 빈 상태/로딩/에러 상태를 안전하게 표시해야 한다.
- docs/TASKS.md에서 Step 21 DONE, Step 22 OPEN 상태로 갱신한다.


## Step 22. Final Verification and Documentation

Status: LOCKED

Goal:
완성본 기준으로 전체 검증을 수행하고, 현재 구현 범위와 제외 범위를 문서화한다.

Scope:
- backend test를 실행한다.
- frontend test를 실행한다.
- frontend build를 실행한다.
- 가능하면 전체 Gradle build를 실행한다.
- README에 로컬 실행 방법을 정리한다.
- README 또는 docs에 Threads 로그인 세션 방식을 설명한다.
- README 또는 docs에 앱이 저장하지 않는 정보를 명시한다:
  - Threads ID
  - 비밀번호
  - 인증코드
  - 쿠키 문자열
  - 세션 토큰
- README 또는 docs에 `./runtime/browser-profiles/threads` 용도를 설명한다.
- README 또는 docs에 현재 구현 범위와 제외 범위를 명시한다.
- docs/TASKS.md의 모든 Step 상태를 최종 정리한다.

Expected verification commands:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:test
npm test
npm run build
```

Do not:
- 새 기능을 추가하지 마라.
- Step 범위를 임의로 확장하지 마라.
- 실제 계정 세션, 쿠키, 토큰, 개인정보를 문서나 테스트에 포함하지 마라.

Verification:
- backend test 통과
- frontend test 통과
- frontend build 통과
- README/docs 업데이트 완료
- docs/TASKS.md에서 Step 22 DONE 상태로 갱신
