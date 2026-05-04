# AI FOMO Dashboard PRD

## 1. 프로젝트 목적

AI 관련 정보가 빠르게 쏟아지면서 생기는 정보 과다와 FOMO를 줄이기 위한 개인용 대시보드를 만든다.

이 앱의 목적은 AI 뉴스를 많이 보여주는 것이 아니다. Threads, 공식 발표, RSS 등에서 수집된 AI 관련 정보를 기준으로 사용자가 실제로 볼 필요가 있는 정보만 남기고, 나에게 적용할 것 / 나중에 볼 것 / 무시해도 되는 것을 분리해서 보여주는 것이다.

## 2. 전체 제품 구조

앱은 탭 기반 단일 웹앱으로 만든다.

초기에는 탭 1만 구현한다.

- 탭 1: 정보 수집·필터링 대시보드
- 탭 2: 프롬프트 저장·관리, 추후 구현
- 탭 N: 기타 확장 기능, 추후 구현

## 3. 이번 구현 범위

이번 구현은 전체 완성품이 아니라 1차 스켈레톤이다.

목표는 수집된 AI 정보를 저장하고, 룰 기반으로 1차 판단해서, 볼 것만 보여주는 대시보드를 만드는 것이다.

이번 범위에 포함한다.

- Source 관리 구조
- CollectedItem 저장 구조
- InfoItem 표시 구조
- Evaluation 저장 구조
- RuleBasedEvaluator 구조
- Dashboard Summary API
- 기본 React 대시보드
- 숨김 항목 보기 토글
- 수동 판단 변경
- 평가 점수 표시

이번 범위에서 제외한다.

- 실제 Threads 크롤링
- 로컬 브라우저 세션 연결
- RSS 수집
- 공식 발표 자동 검증
- LLM API 호출
- TopicCluster 정식 구현
- 회원/로그인
- 수동 InfoItem 추가 폼
- 완전 삭제 기능
- 별도 숨김함 화면
- 삭제 후보 관리 전용 화면
- 사용자 관심사 설정 UI
- 태그 관리 UI

## 4. 정보원 구성

### 4.1 Threads 계정

Threads는 메인 피드다. 이미 사람이 해석하거나 큐레이션한 2차 정보를 중심으로 본다.

초기 Source로 등록할 Threads 계정은 9개다.

| 분류 | URL |
|---|---|
| 뉴스 | https://www.threads.com/@choi.openai |
| 뉴스 | https://www.threads.com/@unclejobs.ai |
| Codex | https://www.threads.com/@appcast |
| Codex | https://www.threads.com/@ethancl |
| Claude | https://www.threads.com/@gptaku_ai |
| Hermes | https://www.threads.com/@roach_log |
| 이미지 AI | https://www.threads.com/@specal1849 |
| 영상 AI | https://www.threads.com/@xazinga |
| 영상 AI | https://www.threads.com/@apple_tea_94 |

이 목록은 DB에 seed로 등록한다. 코드에 하드코딩하지 않는다.

### 4.2 공식 발표 Source

공식 발표 Source는 보완 소스다. 초기 데이터에는 등록하지만, 실제 수집은 나중에 구현한다.

초기 공식 발표 Source 예시:

- OpenAI Official Blog
- Anthropic News
- Google AI Blog

초기에는 active=false 또는 future source 성격으로 둬도 된다.

## 5. 기본 대시보드 표시 정책

초기 화면은 다음 3섹션으로 구성한다.

1. 오늘 볼 것
   - decisionStatus = APPLY
2. 나중에 볼 것
   - decisionStatus = HOLD
3. 검토 필요
   - decisionStatus = UNREVIEWED

IGNORE와 ARCHIVE_CANDIDATE는 기본 목록에서 숨긴다.

단, 사용자가 숨김 항목 보기 토글을 켜면 IGNORE와 ARCHIVE_CANDIDATE도 볼 수 있게 한다.

DELETED 또는 물리 삭제 개념은 초기 버전에서 만들지 않는다.

## 6. 판단 상태

InfoItem의 최종 표시 상태는 decisionStatus로 관리한다.

- UNREVIEWED: 아직 판단되지 않은 정보
- APPLY: 사용자가 바로 볼 가치가 있는 정보
- HOLD: 나중에 볼 가치가 있는 정보
- IGNORE: 볼 필요가 낮아 기본 화면에서 숨기는 정보
- ARCHIVE_CANDIDATE: 중복, 파싱 실패, 무가치 정보 등으로 기본 화면에서 숨기는 정보

초기 버전에서는 물리 삭제를 구현하지 않는다. IGNORE와 ARCHIVE_CANDIDATE는 숨김 또는 아카이브 후보일 뿐이며, DB row를 삭제하지 않는다.

## 7. 중요도

importanceLevel은 다음 값을 가진다.

- HIGH: 현재 작업 흐름에 직접 영향이 크고 바로 확인할 가치가 있음
- MEDIUM: 참고할 가치가 있고 나중에 적용 가능성이 있음
- LOW: 정보성 가치는 있으나 당장 중요하지 않음

## 8. 평가 점수

Evaluation에는 다음 점수를 저장한다.

- relevanceScore: 사용자의 관심사와 관련 있는 정도
- actionabilityScore: 실제 행동으로 이어질 수 있는 정도
- noveltyScore: 새롭거나 중복되지 않는 정도
- confidence: 평가 결과에 대한 확신도

초기 대시보드에서는 이 점수를 카드에 표시한다. 이유는 RuleBasedEvaluator를 튜닝하기 위해 판단 근거가 보여야 하기 때문이다.

## 9. 사용자 수동 판단 우선 원칙

사용자가 직접 decisionStatus를 바꾼 경우, 그 판단은 자동 재평가보다 우선한다.

이를 위해 InfoItem에는 manualOverride 필드를 둔다.

- manualOverride=false: 자동 평가가 decisionStatus를 갱신할 수 있음
- manualOverride=true: 자동 평가가 decisionStatus를 덮어쓰면 안 됨

사용자가 상태를 직접 변경하면 manualOverride=true로 설정한다.

## 10. 중복 처리 정책

초기 버전에서는 정식 TopicCluster를 구현하지 않는다.

대신 최소한 다음 필드를 둔다.

- contentHash
- duplicateOfId
- isDuplicate

중복으로 판단된 항목은 대표 항목만 남기고, 중복 항목은 ARCHIVE_CANDIDATE로 숨긴다.

장기적으로는 여러 InfoItem을 하나의 Topic/Event로 묶어 사용자가 같은 사건을 반복해서 보지 않도록 확장한다.

## 11. 수동 정보 추가 폼 제외

이 앱은 사용자가 직접 정보를 입력하는 앱이 아니다. 사용자는 직접 훑어보고 입력하는 일을 줄이기 위해 이 대시보드를 만든다.

따라서 초기 버전에서는 수동 InfoItem 추가 폼을 만들지 않는다.

## 12. RuleBasedEvaluator v1 기준

초기 자동 판단은 LLM이 아니라 룰 기반으로 한다.

APPLY 후보:

- Codex, Hermes, Claude Code, browser automation, local LLM, AI workflow, developer automation 키워드가 있음
- 사용법, 설정, 업데이트, 워크플로우, 문제 해결, 비교, 튜토리얼 성격이 있음
- 사용자의 개발 자동화 작업에 직접 적용 가능함

HOLD 후보:

- AI 모델 출시
- 이미지/영상 생성 관련 업데이트
- 일반 AI 트렌드
- 도구 소개이지만 당장 적용성은 낮음

IGNORE 후보:

- 감탄형 문장 위주
- 구체 정보가 적은 과장성 글
- 단순 홍보 또는 팔로우 유도
- 사용자의 현재 작업과 관련성이 낮음

ARCHIVE_CANDIDATE 후보:

- contentHash 중복
- 파싱 실패
- 로그인 유도 문구만 있음
- AI와 무관함
- 본문이 너무 짧고 핵심 키워드도 없음

단, 짧은 글이라도 Codex, Hermes, Claude, OpenAI 등 핵심 키워드가 있으면 바로 ARCHIVE_CANDIDATE로 보내지 말고 UNREVIEWED 또는 HOLD로 둔다.

## 13. 기술 스택

### Backend

- Java 21
- Spring Boot 3.x
- Gradle
- Spring Web
- Spring Data JPA
- H2 Database
- Lombok 사용 가능

### Frontend

- React
- Vite
- Tailwind CSS
- 초기 버전에서는 별도 UI 컴포넌트 라이브러리 도입 금지
- 초기 버전에서는 Redux, Zustand 등 상태관리 라이브러리 도입 금지
- React useState/useEffect 중심으로 구현

## 14. 프로젝트 생성 기준

프로젝트 루트 구조는 다음을 기준으로 한다.

```text
ai-fomo-dashboard/
  docs/
  backend/
  frontend/
```

Spring Boot 프로젝트 기준:

- Java version: 21
- Spring Boot: 3.x
- Build tool: Gradle
- Artifact ID: ai-fomo-dashboard
- Base package: com.aifomo.dashboard
- Backend module directory: backend
- Frontend module directory: frontend

## 15. 완료 기준

1차 구현이 끝나면 다음이 가능해야 한다.

- 백엔드가 로컬에서 실행된다.
- H2 console에 접근할 수 있다.
- Source, CollectedItem, InfoItem, Evaluation 테이블이 생성된다.
- Threads 계정 9개가 Source로 seed 된다.
- 공식 발표 Source 샘플이 seed 된다.
- 샘플 CollectedItem 12개와 InfoItem 12개가 1:1로 연결된다.
- 샘플 Evaluation은 evaluatorType=SEED_SAMPLE로 저장된다.
- 대시보드에서 APPLY/HOLD/UNREVIEWED를 기본 표시한다.
- 숨김 항목 보기 토글로 IGNORE/ARCHIVE_CANDIDATE도 확인할 수 있다.
- 사용자가 상태를 수동 변경하면 manualOverride=true가 된다.
