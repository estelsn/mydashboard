# AI FOMO Dashboard 트러블슈팅 기록

이 문서는 `git log`, 커밋 diff, 현재 코드, `README.md`, `docs/RUNBOOK.md`, 테스트 코드를 기준으로 작성했다. 커밋에서 확인되지 않는 원인은 단정하지 않았으며, 중간 해결책이 후속 커밋에서 교체된 경우 그 과정도 함께 기록했다.

개발 과정은 AI-assisted development 환경에서 진행했다. 문제 정의, 우선순위, 실제 동작 검증과 최종 의사결정은 사람이 담당하고, AI는 코드 초안, 원인 후보 탐색, 테스트 초안 작성에 활용했다.

## 대표 트러블슈팅 사례

### 1. 로그인 후에도 Threads 세션이 READY로 유지되지 않음

#### 증상

전용 Chrome에서 Threads 로그인을 완료했지만 Backend 재시작이나 수집 실행 시 세션이 `LOGIN_REQUIRED`로 판정될 수 있었다.

#### 영향

로그인을 마쳤음에도 수집을 시작할 수 없거나, 로그인 브라우저와 수집 브라우저에서 서로 다른 인증 상태를 사용하는 문제가 생겼다.

#### 원인 후보

- 로그인 브라우저와 수집 브라우저의 Chrome 프로필 경로 불일치
- Chrome 내부 프로필 이름 불일치
- macOS Keychain으로 보호된 쿠키를 Playwright가 읽지 못하는 문제
- 단순 HTML 문자열 기준의 잘못된 로그인 판정
- Chrome 프로필 잠금

#### 실제 원인

`51d599e`에서는 프로필 잠금을 피하기 위해 원본 프로필을 임시 디렉터리로 복제해 Playwright를 실행했다. 이 방식은 잠금 충돌은 줄일 수 있지만 로그인 브라우저와 수집 브라우저가 동일한 고정 프로필을 직접 재사용하지 못했다.

후속 커밋 `7f2cbe3`은 로그인과 수집이 동일한 `user-data-dir` 및 `profile-name`을 사용하도록 변경했다. 또한 macOS 쿠키 호환을 위해 Playwright 기본 인자 중 `--use-mock-keychain`을 제외하고, 디렉터리 존재 여부만으로 READY를 판단하지 않고 실제 Threads 페이지의 인증 쿠키와 로그인된 UI를 검사하도록 수정했다.

#### 해결

- 로그인 브라우저와 수집 브라우저에 동일한 고정 Chrome 프로필 적용
- `--profile-directory=Default`, `--restore-last-session` 명시
- Playwright 1.60.0으로 갱신
- `--use-mock-keychain`, `--enable-automation` 기본 인자 제외
- 실제 Threads 페이지를 열어 인증 쿠키, 로그인 UI, 로그인 페이지 여부를 함께 판정
- 프로필 디렉터리만 존재하는 READY 상태도 실제 페이지가 로그인을 요구하면 `LOGIN_REQUIRED`로 강등

#### 검증

- `7f2cbe3` 커밋 기록에 Backend 재시작 후 세션 READY 유지 수동 검증이 명시되어 있다.
- 인증 UI만 존재하는 경우, 쿠키가 있어도 로그인 페이지인 경우, 인증 증거가 없는 경우를 각각 테스트했다.
- 세션 provider가 READY여도 실제 페이지가 로그인 요구 상태이면 `LOGIN_REQUIRED`로 변경되는지 테스트했다.
- 해당 커밋에서 Backend 테스트 82개, Frontend 테스트 36개와 Frontend 프로덕션 빌드 통과가 기록되어 있다.

#### 포트폴리오용 요약

Threads 로그인을 완료해도 수집기에서 세션이 유지되지 않는 문제를 분석했다. 초기 프로필 복제 방식이 로그인 브라우저와 수집 브라우저의 인증 상태 공유를 보장하지 못하는 점과 macOS Keychain 쿠키 호환 문제를 확인했다. 두 브라우저가 동일한 고정 Chrome 프로필을 사용하도록 통합하고 실제 페이지의 쿠키와 로그인 UI를 함께 검사했다. Backend 재시작 후 READY 유지와 인증 상태별 회귀 테스트로 수정 결과를 검증했다.

#### 근거

- 커밋: `51d599e`, `7f2cbe3`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/browser/PlaywrightThreadsBrowserAutomation.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/ThreadsBrowserSessionService.java`
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/session/ThreadsLoginBrowserCommandBuilder.java`
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/session/ThreadsBrowserSessionProperties.java`
  - `backend/src/main/resources/application.yml`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/browser/PlaywrightThreadsBrowserAutomationTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/service/ThreadsBrowserSessionServiceTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/session/ThreadsLoginBrowserCommandBuilderTest.java`

### 2. Chrome 프로필 잠금 회피와 로그인 세션 재사용이 충돌함

#### 증상

로그인용 Chrome이 열려 있거나 잠금 파일이 남아 있으면 Playwright가 `SingletonLock` 관련 오류로 실행되지 않았다. 반대로 잠금을 피하기 위해 프로필을 복제하면 로그인 세션 재사용이 불안정해질 수 있었다.

#### 영향

수집이 시작되지 않고 긴 브라우저 예외가 화면에 노출됐다. 잠금 충돌만 회피하면 인증 세션을 잃을 수 있어 두 요구사항을 동시에 만족해야 했다.

#### 원인 후보

- 실제 Chrome 프로세스가 동일한 프로필 사용 중
- 비정상 종료로 오래된 `SingletonLock` 심볼릭 링크 잔존
- Playwright와 일반 Chrome의 동시 실행
- 잠금 파일 이름만 확인하는 과도한 차단

#### 실제 원인

Chrome persistent profile은 동시에 여러 프로세스에서 열 수 없다. `51d599e`는 잠금 파일을 제외한 임시 프로필 복제로 충돌을 피했지만, `7f2cbe3`에서는 인증 세션을 정확히 재사용하기 위해 원본 고정 프로필 방식으로 돌아갔다. 대신 `SingletonLock`이 가리키는 PID가 실제 실행 중인지 확인해 활성 잠금과 오래된 잠금을 구분했다.

#### 해결

- 임시 프로필 복제 방식은 후속 커밋에서 제거
- 수집 전에 활성 Chrome 프로세스가 프로필을 사용 중인지 검사
- 오래된 `SingletonLock`은 잠금으로 간주하지 않음
- 활성 잠금은 수집을 중단하고 사용자가 로그인 Chrome을 닫도록 안내
- 원시 Playwright 예외를 짧은 한글 메시지로 변환
- Frontend에서 긴 실패 사유를 축약하고 줄바꿈 처리

#### 검증

- 현재 프로세스 PID를 가리키는 잠금은 활성 잠금으로 판정하는 테스트가 있다.
- 존재하지 않는 PID를 가리키는 잠금은 오래된 잠금으로 무시하는 테스트가 있다.
- `SingletonLock` 예외가 사용자용 한글 메시지로 변환되는 테스트가 있다.

#### 포트폴리오용 요약

Chrome 프로필 잠금 문제를 단순 파일 삭제로 처리하지 않고 실제 프로세스 상태와 연결해 판정했다. 초기에는 임시 프로필 복제로 충돌을 피했지만 로그인 쿠키 재사용 요구와 충돌해 고정 프로필 방식으로 교체했다. 활성 PID를 가리키는 잠금만 차단하고 오래된 잠금은 허용하도록 수정했다. 중간 해결책의 한계를 확인하고 후속 설계로 교체한 사례다.

#### 근거

- 커밋: `51d599e`, `7f2cbe3`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/browser/PlaywrightThreadsBrowserAutomation.java`
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/browser/DefaultThreadsBrowserPageClient.java`
  - `frontend/src/ThreadsCollectionPanel.jsx`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/browser/PlaywrightThreadsBrowserAutomationTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/browser/DefaultThreadsBrowserPageClientTest.java`

### 3. 최근 수집에서 오래된 게시물 또는 날짜 불명 게시물이 최신 데이터로 처리됨

#### 증상

Threads 수집 결과에 오래된 게시물이 포함되고, 최근 3일 수집에서도 실제 게시 시각을 알 수 없는 항목이 최신 데이터처럼 저장될 수 있었다.

#### 영향

사용자는 최근 정보를 요청했지만 오래된 고정 게시물이나 날짜를 판정할 수 없는 항목을 다시 검토해야 했다.

#### 원인 후보

- Threads의 고정 게시물이 최신 글보다 먼저 노출됨
- 상대 시각과 한글 날짜 파싱 실패
- 게시 시각 대신 수집 시각을 사용
- 스크롤 부족으로 최신 게시물 후보를 충분히 확보하지 못함

#### 실제 원인

`587ca1a` 이전에는 게시물의 표시 시각을 최근 필터 기준으로 사용할 수 있는 해석기가 없었다. 이 커밋에서 ISO 시각, 영문·한글 상대 시각, 연도가 없는 월·일을 해석하는 `ThreadsPostDateResolver`와 최근 3일 필터가 추가됐다.

그러나 해석 실패 시 `publishedAt`에 `collectedAt`을 대입하는 fallback이 남아 있어 날짜를 알 수 없는 게시물이 수집 시점의 최신 글로 오인될 수 있었다. `c3f591f`에서 fallback을 `null`로 변경했고 최근 수집은 `publishedAt == null`인 항목을 제외하도록 유지했다.

#### 해결

- ISO, 영문·한글 상대 시각, 월·일 형식 지원
- 해석된 게시 시각 기준 최신순 정렬
- 최근 3일보다 오래된 게시물 제외
- 날짜 해석 실패 항목은 수집 시각으로 대체하지 않고 `null` 유지
- 최근 수집에서 날짜 불명 항목 제외
- 요청 계정의 canonical post URL만 추출해 다른 계정 게시물 혼입 방지

#### 검증

- ISO, `3h`, `어제`, `May 5`, `5월 5일`, 알 수 없는 문자열을 고정 Clock으로 테스트했다.
- 최근 1일, 4일 전, 날짜 불명 게시물을 함께 입력해 최근 항목만 저장되는지 테스트했다.
- 날짜 해석 실패 시 `publishedAt`이 `null`인지 테스트했다.
- 요청한 계정 handle이 구조화 추출 함수에 전달되는지 테스트했다.

#### 포트폴리오용 요약

최근 수집에서 고정 게시물과 날짜 불명 항목이 최신 글처럼 처리되는 문제를 해결했다. 다양한 날짜 표현을 절대 시각으로 변환하고 최근 3일 필터를 적용했으며, 해석 실패 시 수집 시각을 게시 시각으로 사용하던 fallback을 제거했다. 요청 계정의 게시물만 추출하도록 작성자 필터도 추가했다. 고정 Clock 기반 단위 테스트와 최근·오래된·날짜 불명 데이터를 함께 사용하는 서비스 테스트로 검증했다.

#### 근거

- 커밋: `587ca1a`, `c3f591f`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/ThreadsPostDateResolver.java`
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/browser/ThreadsBrowserCollector.java`
  - `backend/src/main/java/com/aifomo/dashboard/collector/threads/browser/PlaywrightThreadsBrowserAutomation.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/ManualThreadsCollectionService.java`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/ThreadsPostDateResolverTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/browser/ThreadsBrowserCollectorTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/collector/threads/browser/PlaywrightThreadsBrowserAutomationTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/service/ManualThreadsCollectionServiceTest.java`

### 4. 같은 게시물이 시간·URL 표현 변화 때문에 중복 저장됨

#### 증상

동일한 Threads 게시물을 다시 수집할 때 본문에 포함된 상대 시각이나 URL 표현이 달라지면 다른 콘텐츠로 판단될 수 있었다.

#### 영향

같은 정보가 여러 `InfoItem`으로 저장되어 검토 대상이 늘고, 새 데이터 저장 건수와 중복 건수의 신뢰도가 낮아졌다.

#### 원인 후보

- 원문 전체 문자열을 그대로 해시
- 상대 날짜가 수집 시점마다 변경
- URL query 또는 canonical URL 차이
- 공백과 대소문자 차이

#### 실제 원인

초기 저장 로직은 정규화된 전체 원문에 SHA-256을 적용했다. 공백과 대소문자는 정규화했지만 URL과 상대·절대 날짜가 원문에 남아 있어 같은 게시물도 수집 시점에 따라 다른 해시가 생성될 수 있었다.

#### 해결

- URL 제거
- 영문·한글 상대 시각 제거
- 절대 날짜 표현 제거
- 남은 본문을 공백·대소문자 정규화 후 SHA-256 처리
- 콘텐츠 해시뿐 아니라 원문 URL도 중복 기준으로 사용
- 본문이 비어 있으면 URL을 fallback 키로 사용

#### 검증

- 기존 콘텐츠 해시가 있으면 새 `InfoItem`을 만들지 않는 테스트가 있다.
- 같은 post URL에서 상대 시각 문자열이 달라져도 `createdCount=0`, `duplicateCount=1`인지 테스트했다.
- 중복 실행은 `SUCCEEDED`일 수 있지만 생성 0건과 중복 1건이 별도 집계되는지 확인했다.

#### 포트폴리오용 요약

동일 게시물의 상대 시각과 URL 표현이 바뀌어 중복 저장되는 문제를 분석했다. 원문 전체 해시 대신 URL과 날짜 표현을 제거한 본문 중심 canonical key를 만들고 SHA-256을 적용했다. 원문 URL도 별도 중복 기준으로 사용해 해시와 URL을 상호 보완했다. 시간 문자열이 달라진 동일 게시물에서 새 항목이 생성되지 않는 테스트로 검증했다.

#### 근거

- 커밋: `587ca1a`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/service/ThreadsCollectionPersistenceService.java`
  - `backend/src/main/java/com/aifomo/dashboard/util/ContentHashUtil.java`
  - `backend/src/main/java/com/aifomo/dashboard/repository/CollectedItemRepository.java`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/service/ThreadsCollectionPersistenceServiceTest.java`

### 5. 로그인 브라우저 열기 요청이 CORS 403으로 실패함

#### 증상

Frontend에서 `POST /api/threads/session/open-login`을 호출하면 CORS preflight 단계에서 403이 발생했다.

#### 영향

Backend API가 구현되어 있어도 사용자는 UI에서 로그인 Chrome을 열 수 없었다. 빈 403 응답에서는 어느 요청이 실패했는지 확인하기도 어려웠다.

#### 원인 후보

- Backend 미실행
- 잘못된 API base URL
- Vite proxy 설정 오류
- CORS origin 불일치
- CORS 허용 메서드에서 POST 누락

#### 실제 원인

`CorsConfig`는 `http://localhost:5173` origin을 허용했지만 메서드는 `GET`, `PATCH`, `OPTIONS`만 허용하고 있었다. 로그인 브라우저 API는 POST이므로 preflight가 거부됐다.

#### 해결

- CORS 허용 메서드에 POST 추가
- 로그인 브라우저 실행 성공 응답을 `202 Accepted`로 명확화
- `LOGIN_REQUIRED`는 HTTP 403이 아니라 정상 세션 상태 payload로 표현
- Frontend 오류 메시지에 HTTP method와 API path 포함
- 로그인 브라우저 열기 실패와 세션 상태 재확인 실패를 별도 상태로 표시

#### 검증

- `Origin: http://localhost:5173`, `Access-Control-Request-Method: POST` preflight가 성공하고 허용 origin을 반환하는지 MockMvc로 테스트했다.
- 로그인 브라우저 API가 `202 Accepted`를 반환하는지 테스트했다.
- 빈 403 응답에서도 `POST /api/threads/session/open-login failed with HTTP 403` 메시지가 생성되는지 Frontend 테스트로 확인했다.

#### 포트폴리오용 요약

로그인 브라우저 API 자체는 동작하지만 Frontend 호출만 403으로 실패하는 문제를 CORS preflight 단계로 좁혔다. 허용 origin은 맞았지만 POST가 허용 메서드에서 누락된 것이 실제 원인이었다. POST 허용, 202 응답, API 경로가 포함된 오류 메시지를 적용했다. Backend preflight 테스트와 Frontend 빈 403 응답 테스트로 재발을 방지했다.

#### 근거

- 커밋: `3c24256`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/config/CorsConfig.java`
  - `backend/src/main/java/com/aifomo/dashboard/controller/ThreadsSessionController.java`
  - `frontend/src/api/dashboardApi.js`
  - `frontend/src/ThreadsSessionPanel.jsx`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/controller/ThreadsSessionApiTest.java`
  - `frontend/src/api/dashboardApi.test.js`
  - `frontend/src/ThreadsSessionPanel.test.jsx`

### 6. 일부 출처 실패가 전체 실패로만 표시되고 생성 0건 원인을 구분하기 어려움

#### 증상

여러 출처를 수집할 때 한 계정의 접근 제한이나 타임아웃만 발생해도 전체 실행이 실패로 표시됐다. 실행이 성공으로 끝나도 생성 건수가 0이면 빈 결과, 중복, 쿨다운 중 어느 원인인지 실행 단위 정보만으로 구분하기 어려웠다.

#### 영향

정상적으로 수집된 다른 출처의 결과가 가려졌고, 사용자는 수집 로직 실패와 정상적인 0건 생성을 구별하기 어려웠다.

#### 원인 후보

- 첫 실패에서 전체 루프 중단
- 전체 실행 상태만 저장
- 수집, 생성, 중복, 저장 실패 집계 부족
- 계정별 결과 모델 부재

#### 실제 원인

기존 상태 계산은 `failedSourceCount > 0` 또는 저장 실패가 있으면 전체 실행을 `FAILED`로 만들었다. 또한 `CollectionRun`만 존재하고 계정별 결과 엔티티가 없어 어느 출처가 어떤 상태와 건수를 반환했는지 구조적으로 보존하지 않았다.

#### 해결

- `CollectionSourceResult` 엔티티와 응답 DTO 추가
- 출처별 상태, 수집, 생성, 중복, 실패 건수와 메시지 저장
- `PARTIAL_SUCCESS` 상태 추가
- 특정 계정의 접근 제한과 타임아웃은 기록 후 다음 출처 계속 수집
- 로그인 필요와 활성 프로필 잠금만 공용 세션 실패로 보고 전체 중단
- 수집, 파싱, 중복, 저장 실패 단계별 로그와 집계 추가
- Frontend 실행 카드를 펼치면 출처별 결과를 확인하도록 변경

#### 검증

- 첫 출처가 `ACCESS_RESTRICTED` 또는 `TIMEOUT`이어도 두 번째 출처를 수집하는지 테스트했다.
- 일부 실패와 일부 생성이 함께 있으면 `PARTIAL_SUCCESS`인지 확인했다.
- API 응답에 `sourceResults`와 출처 이름이 포함되는지 테스트했다.
- 현재 테스트는 중복만 발생한 실행이 `SUCCEEDED`, `createdCount=0`, `duplicateCount=1`이 될 수 있음을 명시한다.

#### 포트폴리오용 요약

다중 출처 수집에서 일부 계정 실패가 전체 실패로만 표시되는 문제를 해결했다. 전체 실행과 출처별 결과를 분리하고 `PARTIAL_SUCCESS` 상태를 도입했다. 계정 접근 제한과 타임아웃은 다음 출처 수집을 계속하되, 로그인 만료와 활성 프로필 잠금만 전체 중단 조건으로 구분했다. 출처별 상태와 생성·중복·실패 건수를 API와 UI에 노출해 생성 0건의 원인을 추적할 수 있게 했다.

#### 근거

- 커밋: `7f2cbe3`, `c3f591f`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/domain/collection/CollectionRunStatus.java`
  - `backend/src/main/java/com/aifomo/dashboard/domain/collection/CollectionSourceResult.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/ManualThreadsCollectionService.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/ThreadsCollectionPersistenceService.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/CollectionRunQueryService.java`
  - `frontend/src/ThreadsCollectionPanel.jsx`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/service/ManualThreadsCollectionServiceTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/service/ThreadsCollectionPersistenceServiceTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/controller/CollectionRunApiTest.java`
  - `frontend/src/ThreadsCollectionPanel.test.jsx`

### 7. 수집 데이터가 검토 필요에 머물고 Evaluation STUB 경계가 불명확함

#### 증상

수집된 정보가 `UNREVIEWED`에 많이 남을 수 있었고, enum 이름만 보면 규칙 기반 평가와 LLM 평가가 모두 STUB처럼 보였다. 평가 이유도 기존 데이터에는 영문으로 남아 있었다.

#### 영향

`오늘 볼 것`과 `나중에 볼 것` 분류가 충분히 채워지지 않아 대시보드의 핵심 분류 흐름이 약해졌다. 구현된 규칙 평가와 미구현 LLM 평가의 경계도 설명하기 어려웠다.

#### 원인 후보

- 수집 후 평가 서비스 미호출
- 일반 AI 뉴스에 대한 HOLD 규칙 부족
- 짧은 콘텐츠와 thread fragment 판정 부족
- 출처명 `openai`가 본문 평가 신호로 섞임
- 부분 문자열 `ai`가 `Taipei` 같은 단어에도 일치
- 규칙 평가기와 LLM 평가기 모두 실제 미구현

#### 실제 원인

초기 `rule-v1`은 핵심·행동 키워드나 특정 HOLD 신호에 걸리지 않는 AI 콘텐츠를 최종적으로 `UNREVIEWED`로 반환했다. 문자열 포함 검사로 `ai`를 찾고 출처 카테고리와 출처 식별자를 평가 텍스트에 포함해 잘못된 신호가 생길 수 있었다. 또한 Threads 저장 후 평가 파이프라인을 자동 호출하지 않았다.

`cbc3bcd`에서 Evaluation Pipeline과 `LLM_READY_STUB`이 추가됐으며, 커밋 메시지는 외부 LLM 호출을 명시적으로 제외했다. 현재 `RULE_BASED_STUB` enum 이름은 남아 있지만 실제 규칙 평가는 구현되어 있다. `c3f591f`에서 `rule-v3`로 일반 AI 정보를 HOLD로 분류하고, 실행 가능한 개발 정보는 APPLY, 홍보성 정보는 IGNORE, 불완전 thread는 UNREVIEWED로 분리했다. 출처 식별자 제거, 단어 경계 검사, 한글 이유, 수집 직후 자동 평가도 추가했다.

#### 해결

- Threads 저장 직후 `recalculateUnreviewedItems()` 실행
- 일반 AI 정보의 HOLD 규칙 추가
- 실행 가능한 개발 정보 APPLY 분류 강화
- 홍보성, 짧은 본문, thread fragment 규칙 분리
- URL, 날짜, 작성자 handle, 출처명 제거 후 평가
- 영문 단어는 단어 경계 기준으로 검사
- 기존 영문 평가 이유를 한글로 변환
- 실제 규칙 평가와 외부 호출이 없는 LLM STUB을 문서에서 명확히 구분

#### 검증

- 실행 가능한 개발 정보가 APPLY인지 테스트했다.
- 일반 AI 뉴스가 UNREVIEWED가 아니라 HOLD인지 테스트했다.
- 홍보성 정보, 불완전 thread, `Taipei` 오탐, 작성자 handle 영향을 각각 테스트했다.
- 수집 직후 새 항목이 `rule-v3`로 평가되는지 서비스 테스트로 확인했다.
- 단건 및 일괄 재평가 API와 수동 결정 보호를 테스트했다.

#### 포트폴리오용 요약

수집 데이터가 검토 필요에 머무는 문제를 규칙 신호와 파이프라인 연결 관점에서 분석했다. 일반 AI 정보, 실행 가능한 개발 정보, 홍보성 정보, 불완전 thread를 분리하는 `rule-v3`를 적용하고 출처명과 부분 문자열 오탐을 제거했다. 수집 직후 평가를 실행하고 한글 근거를 저장하도록 변경했다. 규칙 평가는 실제 구현이며 외부 모델 호출이 없는 `LLM_READY_STUB`만 미구현이라는 경계도 명확히 정리했다.

#### 근거

- 커밋: `9bc476f`, `cbc3bcd`, `c3f591f`
- 관련 파일:
  - `backend/src/main/java/com/aifomo/dashboard/service/RuleBasedEvaluator.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/EvaluationService.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/EvaluationReasonLocalizer.java`
  - `backend/src/main/java/com/aifomo/dashboard/domain/evaluation/EvaluatorType.java`
  - `backend/src/main/java/com/aifomo/dashboard/service/ManualThreadsCollectionService.java`
- 관련 테스트:
  - `backend/src/test/java/com/aifomo/dashboard/service/RuleBasedEvaluatorTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/controller/EvaluationApiTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/controller/RuleBasedEvaluationApiTest.java`
  - `backend/src/test/java/com/aifomo/dashboard/service/ManualThreadsCollectionServiceTest.java`

## 후보 검토 결과와 남은 제한

### Frontend API ECONNREFUSED

`docs/RUNBOOK.md`, `README.md`, Vite proxy 설정에서 Backend `localhost:8080`과 Frontend `localhost:5173` 연결 조건은 확인된다. 그러나 커밋 메시지와 diff에는 특정 `ECONNREFUSED` 장애의 재현 원인과 코드 수정이 기록되어 있지 않다. 따라서 대표 해결 사례로 작성하지 않고, Backend 미실행 또는 잘못된 API 주소를 확인하는 로컬 실행 진단 항목으로만 분류한다.

근거:

- `docs/RUNBOOK.md`
- `README.md`
- `frontend/vite.config.js`
- `backend/src/main/java/com/aifomo/dashboard/config/CorsConfig.java`

### 8080 포트 충돌

README에는 `lsof`를 이용한 포트 점유 확인 절차가 있지만, 특정 포트 충돌을 해결한 커밋 diff는 없다. 실행 환경에서 발생 가능한 운영 이슈이며 코드 결함 해결 사례로 과장하지 않는다.

근거:

- `README.md`
- `backend/src/main/resources/application.yml`

### 성공 상태이지만 생성 0건

현재 구현에서도 다음 경우 `SUCCEEDED`와 `createdCount=0`이 함께 발생할 수 있다.

- 모든 후보가 기존 URL 또는 contentHash 중복인 경우
- 출처가 `EMPTY_RESULT`를 반환한 경우
- 오류 없이 저장할 신규 게시물이 없는 경우

이는 항상 저장 결함을 의미하지 않는다. `CollectionRun`의 성공은 실행 오류 여부를 나타내고, 신규 생성 여부는 `createdCount`, `duplicateCount`, 출처별 상태로 별도 판단해야 한다. 현재 개선은 원인 가시성 확보이며, `SUCCEEDED_WITH_NO_CHANGES` 같은 별도 상태는 구현되어 있지 않다.

근거:

- `backend/src/main/java/com/aifomo/dashboard/service/ThreadsCollectionPersistenceService.java`
- `backend/src/test/java/com/aifomo/dashboard/service/ThreadsCollectionPersistenceServiceTest.java`
- `backend/src/main/java/com/aifomo/dashboard/domain/collection/CollectionSourceResult.java`

### 비정상 종료된 RUNNING 상태

현재 `CollectionRunRepository.existsByStatus(RUNNING)`은 중복 실행을 차단하지만 비정상 종료된 실행을 자동 만료하거나 복구하는 로직은 없다. README의 실행 이력 삭제 절차는 운영상 우회 방법이며 자동 복구가 구현된 것은 아니다.

근거:

- `backend/src/main/java/com/aifomo/dashboard/repository/CollectionRunRepository.java`
- `backend/src/main/java/com/aifomo/dashboard/service/ManualThreadsCollectionService.java`
- `backend/src/main/java/com/aifomo/dashboard/service/CollectionRunQueryService.java`
- `README.md`

## 통합 포트폴리오용 트러블슈팅 요약 TOP 3

### TOP 1. Threads 로그인 세션 READY 안정화

전용 Chrome에서 로그인해도 수집기가 세션을 재사용하지 못해 `LOGIN_REQUIRED`로 돌아가는 문제가 있었다. 프로필 경로, Chrome 내부 프로필, macOS Keychain 쿠키, 로그인 판정 방식을 각각 분리해 분석했다. 초기 임시 프로필 복제 방식은 잠금 충돌은 줄였지만 인증 상태 공유에 한계가 있어 후속 커밋에서 교체했다. 로그인과 수집이 같은 고정 프로필을 사용하도록 통합하고 Playwright의 mock keychain 인자를 제외했다. 디렉터리 존재 여부가 아니라 실제 페이지의 인증 쿠키와 로그인 UI를 함께 검사하도록 변경했다. Backend 재시작 후 READY 유지, 활성·오래된 프로필 잠금, 로그인 페이지 판정 테스트로 결과를 검증했다.

근거: `51d599e`, `7f2cbe3`

### TOP 2. 최근 게시물 판정과 중복 제거 정확도 개선

최근 수집에 오래된 게시물이 섞이고 동일 게시물이 시간 표현 변화로 다시 저장되는 문제가 있었다. Threads의 고정 게시물, 상대 날짜 파싱 실패, 게시 시각 대신 수집 시각을 사용하는 fallback, 원문 전체 해시를 원인으로 분리했다. ISO와 한글·영문 상대 시각을 해석하고 최근 3일 필터를 적용했다. 날짜를 해석하지 못한 항목은 최신으로 간주하지 않고 제외했으며 요청 계정의 게시물만 추출했다. URL과 날짜 표현을 제거한 본문 canonical key에 SHA-256을 적용하고 원문 URL도 중복 기준으로 사용했다. 날짜 형식별 단위 테스트와 상대 시각이 달라진 동일 게시물 중복 테스트로 결과를 검증했다.

근거: `587ca1a`, `c3f591f`

### TOP 3. 다중 출처 수집의 부분 성공과 원인 가시성 확보

여러 출처 중 한 계정이 실패하면 전체 실행이 실패로만 표시되고 생성 0건의 원인을 구분하기 어려웠다. 전체 실행 상태만 저장하고 출처별 결과 모델이 없었던 것이 실제 구조적 원인이었다. `CollectionSourceResult`와 `PARTIAL_SUCCESS`를 추가해 출처별 상태와 수집·생성·중복·실패 건수를 저장했다. 접근 제한과 타임아웃은 해당 출처 실패로 격리하고, 로그인 만료와 프로필 잠금만 전체 중단 조건으로 구분했다. API와 접을 수 있는 실행 카드에서 출처별 결과를 확인하도록 연결했다. 일부 실패 후 다음 출처 성공, 부분 성공 상태, API의 `sourceResults` 반환을 테스트해 검증했다.

근거: `7f2cbe3`, `c3f591f`
