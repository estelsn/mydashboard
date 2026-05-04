# AI FOMO Dashboard Architecture

## 1. 기본 원칙

이 프로젝트는 수집기를 먼저 만드는 프로젝트가 아니다. 핵심은 수집된 정보를 저장하고, 중복과 저가치 정보를 줄이고, 사용자가 볼 가치가 있는 정보만 대시보드에 남기는 것이다.

초기 구조는 다음 레이어로 나눈다.

- Core Dashboard Layer
- Ingestion Preparation Layer
- Evaluation Layer

실제 Threads 수집, RSS 수집, 공식 발표 매칭, LLM 평가는 초기 구현 범위에서 제외한다.

## 2. 프로젝트 폴더 구조

```text
ai-fomo-dashboard/
  docs/
    PRD.md
    ARCHITECTURE.md
    TASKS.md
    THREADS_EXTRACTION_NOTES.md
  backend/
  frontend/
```

문서는 반드시 프로젝트 루트의 docs 디렉터리에 둔다.

Codex 시작 프롬프트에서 참조하는 경로는 다음과 같다.

- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/THREADS_EXTRACTION_NOTES.md

## 3. Backend 기준

- Java 21
- Spring Boot 3.x
- Gradle
- Base package: com.aifomo.dashboard
- Artifact ID: ai-fomo-dashboard
- Module directory: backend

권장 패키지 구조:

```text
com.aifomo.dashboard
  config
  domain
    source
    collected
    info
    evaluation
  repository
  service
  controller
  dto
  seed
```

초기 Step 1에서는 Controller/API를 구현하지 않는다. Step 1은 domain, enum, repository, seed, H2 설정까지만 구현한다.

## 4. Frontend 기준

- React
- Vite
- Tailwind CSS
- Module directory: frontend
- 개발 서버 기본 포트: 5173

초기 버전에서는 별도 UI 컴포넌트 라이브러리를 도입하지 않는다.

초기 버전에서는 Redux, Zustand 등 상태관리 라이브러리를 도입하지 않는다.

## 5. application.yml 기준

백엔드 기본 설정은 다음 기준을 따른다.

```yaml
server:
  port: 8080

spring:
  h2:
    console:
      enabled: true
      path: /h2-console
  datasource:
    url: jdbc:h2:file:./data/aifomo
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

H2 console URL:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:file:./data/aifomo
```

## 6. CORS 정책

Step 2에서 API를 만들 때 로컬 프론트엔드 개발 서버를 허용한다.

허용 origin:

```text
http://localhost:5173
```

CORS는 WebMvcConfigurer 또는 컨트롤러 단위 설정 중 단순한 방식으로 구현한다.

Step 1에서는 Controller/API가 없으므로 CORS 구현은 필수 범위가 아니다.

## 7. 공통 엔티티 기준

모든 엔티티 id는 초기 버전에서 Long auto increment를 사용한다.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

날짜 타입은 LocalDateTime을 사용한다.

- createdAt
- updatedAt
- collectedAt
- publishedAt
- deletedAt, 초기 버전에서는 사용하지 않음

초기 버전에서는 timezone 변환을 다루지 않는다.

## 8. Source

정보 출처를 나타낸다.

필드:

- id: Long
- name: String
- sourceType: SourceType
- category: SourceCategory
- url: String
- description: String
- isActive: boolean
- lastCollectedAt: LocalDateTime nullable
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

SourceType:

- THREADS
- OFFICIAL_BLOG
- RSS
- DOCS
- ETC

SourceCategory:

- NEWS
- CODEX
- CLAUDE
- HERMES
- IMAGE
- VIDEO
- COMPANY_OFFICIAL
- ETC

## 9. CollectedItem

수집 원본을 저장한다.

필드:

- id: Long
- source: Source
- rawUrl: String
- rawContent: String
- contentHash: String
- status: CollectedItemStatus
- collectedAt: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

CollectedItemStatus:

- COLLECTED
- PARSE_FAILED
- DUPLICATE
- IGNORED

## 10. contentHash 생성 기준

contentHash는 normalized rawContent의 SHA-256 해시다.

정규화 기준:

1. rawContent가 null이면 seed 실패 또는 validation 실패로 본다.
2. trim 처리한다.
3. 연속 공백을 하나의 공백으로 치환한다.
4. 소문자로 변환한다.

rawUrl은 contentHash 계산에 포함하지 않는다. 같은 내용이 다른 URL로 들어와도 중복으로 잡기 위함이다.

샘플 데이터에서도 동일한 규칙으로 contentHash를 생성한다. UUID나 임의 문자열을 contentHash로 넣지 않는다.

## 11. InfoItem

대시보드에 표시할 정제 정보다.

필드:

- id: Long
- source: Source
- collectedItem: CollectedItem
- title: String
- summary: String
- originalUrl: String
- category: SourceCategory
- tags: String
- importanceLevel: ImportanceLevel
- decisionStatus: DecisionStatus
- manualOverride: boolean
- isHidden: boolean
- isDeleted: boolean
- duplicateOfId: Long nullable
- isDuplicate: boolean
- publishedAt: LocalDateTime nullable
- collectedAt: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

초기 버전에서 tags는 JSON 문자열로 저장한다.

예:

```json
["codex", "workflow", "automation"]
```

별도 Tag 테이블은 만들지 않는다.

DecisionStatus:

- UNREVIEWED
- APPLY
- HOLD
- IGNORE
- ARCHIVE_CANDIDATE

ImportanceLevel:

- HIGH
- MEDIUM
- LOW

## 12. Evaluation

InfoItem에 대한 평가 결과를 저장한다.

필드:

- id: Long
- infoItem: InfoItem
- decisionStatus: DecisionStatus
- reason: String
- confidence: double
- relevanceScore: double
- actionabilityScore: double
- noveltyScore: double
- evaluatorType: EvaluatorType
- evaluatorVersion: String
- createdAt: LocalDateTime

EvaluatorType:

- SEED_SAMPLE
- RULE_BASED
- MANUAL
- LLM

Step 1에서 seed로 만드는 Evaluation은 evaluatorType=SEED_SAMPLE을 사용한다.

Step 3에서 RuleBasedEvaluator가 실제로 만든 Evaluation은 evaluatorType=RULE_BASED, evaluatorVersion=rule-v1을 사용한다.

## 13. 수동 판단 우선 정책

사용자가 InfoItem의 decisionStatus를 직접 변경하면 다음을 적용한다.

- InfoItem.manualOverride = true
- InfoItem.decisionStatus = 사용자가 선택한 상태
- 필요 시 evaluatorType=MANUAL Evaluation을 추가할 수 있음

자동 평가기는 manualOverride=true인 InfoItem의 decisionStatus를 덮어쓰면 안 된다.

## 14. 숨김/아카이브 정책

초기 버전에서는 물리 삭제를 구현하지 않는다.

DELETE /api/info-items/{id}는 만들지 않거나, 만들더라도 soft delete만 수행한다.

권장 API는 다음과 같다.

- PATCH /api/info-items/{id}/archive
- PATCH /api/info-items/{id}/restore

IGNORE와 ARCHIVE_CANDIDATE는 기본 대시보드에서 숨긴다.

숨김 항목 보기 토글을 켠 경우에만 IGNORE와 ARCHIVE_CANDIDATE를 표시한다.

## 15. 중복 처리 정책

초기 버전에서는 TopicCluster를 구현하지 않는다.

중복 처리는 contentHash 기준으로 시작한다.

- 같은 contentHash가 이미 있으면 새 CollectedItem.status=DUPLICATE
- 대응 InfoItem은 isDuplicate=true
- duplicateOfId에 대표 InfoItem id를 저장
- decisionStatus=ARCHIVE_CANDIDATE
- isHidden=true

장기적으로는 TopicCluster 또는 Event 단위 묶음으로 확장할 수 있다.

## 16. DataSeeder 기준

DataSeeder는 CommandLineRunner로 구현한다.

중복 삽입 방지 조건:

- Source 데이터가 이미 존재하면 seed를 다시 넣지 않는다.
- 또는 URL 기준으로 Source 중복을 방지한다.
- 샘플 CollectedItem/InfoItem/Evaluation도 실행할 때마다 무한히 중복 생성되지 않게 한다.

Step 1 seed 데이터:

- Threads Source 9개
- 공식 발표 Source 샘플 3개
- CollectedItem 12개
- InfoItem 12개
- Evaluation 12개

샘플 데이터 관계:

- CollectedItem 12개와 InfoItem 12개는 1:1로 연결한다.
- InfoItem.collectedItem은 null로 두지 않는다.
- Evaluation 12개는 각각의 InfoItem에 연결한다.
- seed Evaluation의 evaluatorType은 SEED_SAMPLE이다.

## 17. API 설계, Step 2 이후

Step 1에서는 API를 구현하지 않는다.

Step 2에서 구현할 API:

Source:

- GET /api/sources

InfoItem:

- GET /api/info-items
- GET /api/info-items/{id}
- PATCH /api/info-items/{id}/decision
- PATCH /api/info-items/{id}/archive
- PATCH /api/info-items/{id}/restore

Dashboard:

- GET /api/dashboard/summary

Evaluation:

- POST /api/info-items/{id}/evaluate
- POST /api/info-items/evaluate-batch

## 18. Dashboard UI 구조

기본 화면:

- 상단 요약 카드
- 오늘 볼 것: APPLY
- 나중에 볼 것: HOLD
- 검토 필요: UNREVIEWED
- 숨김 항목 보기 토글
- 숨김 항목: IGNORE, ARCHIVE_CANDIDATE

카드 표시 항목:

- 제목
- 요약
- 출처
- 카테고리
- 원문 URL
- decisionStatus
- importanceLevel
- tags
- Evaluation reason
- relevanceScore
- actionabilityScore
- noveltyScore
- confidence

정렬 기준:

1. decisionStatus=APPLY 우선
2. importanceLevel=HIGH 우선
3. relevanceScore 높은 순
4. actionabilityScore 높은 순
5. collectedAt 최신순

## 19. Threads 수집 관련 설계

Threads 수집은 향후 구현한다.

상세 실험 기록은 docs/THREADS_EXTRACTION_NOTES.md에 둔다.

중요:

- Step 1~6에서는 Threads 수집 코드를 구현하지 않는다.
- Step 1~6에서는 Chrome 실행 코드를 작성하지 않는다.
- Step 1~6에서는 curl/GraphQL 호출 코드를 작성하지 않는다.
- Step 1~6에서는 Playwright/Puppeteer를 추가하지 않는다.
- THREADS_EXTRACTION_NOTES.md는 참고 자료이며, 구현 지시가 아니다.

## 20. 테스트 기준

Step 1에서는 자동 테스트 작성이 필수는 아니다.

Step 1 완료 확인 기준:

- Spring Boot 앱이 실행된다.
- H2 console에 접근할 수 있다.
- 테이블이 생성된다.
- Source seed 데이터가 확인된다.
- CollectedItem, InfoItem, Evaluation 샘플 데이터가 확인된다.

Step 2 이후 API 구현부터 간단한 service/repository 테스트를 추가할 수 있다.
