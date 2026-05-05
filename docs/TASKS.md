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

Status: OPEN

Step 4 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- 상태 변경 버튼
- manualOverride 반영
- 숨김/복구 버튼
- 평가 점수 표시
- 평가 이유 표시

## Step 6. CollectionRun Skeleton

Status: LOCKED

Step 5 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- CollectionRun 엔티티 설계
- 수집 상태 표시용 구조만 구현
- 실제 수집기는 구현하지 않음

## Step 7. Threads Collector Design Only

Status: LOCKED

Step 6 검토 전에는 구현하지 않는다.

### 구현 예정 범위

- Threads collector interface 설계
- THREADS_EXTRACTION_NOTES.md 기반 구현 계획 작성
- 실제 Chrome 실행 코드 작성 금지
- 실제 Threads 접속 코드 작성 금지

## Codex 시작 프롬프트

아래 내용을 Codex에 입력한다.

```text
너는 이 프로젝트의 구현 담당자다.

먼저 다음 문서를 순서대로 읽어라.

1. docs/PRD.md
2. docs/ARCHITECTURE.md
3. docs/TASKS.md
4. docs/THREADS_EXTRACTION_NOTES.md

중요:
- 이번 작업은 docs/TASKS.md의 Step 1만 수행한다.
- Step 2 이후 작업은 절대 구현하지 않는다.
- LOCKED 단계는 구현하지 않는다.
- 실제 Threads 크롤링, 로컬 브라우저 세션, Chrome 실행, Playwright/Puppeteer, curl, GraphQL 호출, RSS, LLM 연동, 공식 발표 매칭은 구현하지 않는다.
- docs/THREADS_EXTRACTION_NOTES.md는 참고 자료다. 지금 구현하지 않는다.
- 실행 가능한 작은 단위로만 작업한다.

Step 1 완료 기준:
- backend Spring Boot 프로젝트가 있다.
- Java 21, Spring Boot 3.x, Gradle 기준이다.
- base package는 com.aifomo.dashboard다.
- H2 file DB 설정이 되어 있다.
- server port는 8080이다.
- H2 console path는 /h2-console이다.
- Source, CollectedItem, InfoItem, Evaluation 엔티티가 있다.
- 필요한 Enum이 있다.
- Repository가 있다.
- DataSeeder는 CommandLineRunner로 구현한다.
- seed 데이터 중복 삽입을 방지한다.
- Threads Source 9개, 공식 발표 Source 3개가 seed 된다.
- CollectedItem 12개와 InfoItem 12개가 1:1로 연결된다.
- Evaluation 12개가 seed 되고 evaluatorType은 SEED_SAMPLE이다.
- contentHash는 SHA-256(normalized rawContent)로 생성한다.
- 앱이 정상 실행된다.

완료 후 다음을 보고하라.
1. 생성/수정한 파일 목록
2. 구현한 내용 요약
3. 실행 방법
4. H2 console 확인 방법
5. seed 데이터 확인 방법
6. Step 1에서 의도적으로 구현하지 않은 것
7. 다음 Step에서 해야 할 일
```
