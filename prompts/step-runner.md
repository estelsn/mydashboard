너는 이 프로젝트의 구현 담당자다.

이번 작업:
- docs/TASKS.md에서 Status: OPEN인 Step 하나만 찾아 수행하라.
- OPEN Step이 여러 개면 가장 번호가 낮은 Step 하나만 수행하라.
- LOCKED Step은 구현하지 마라.
- DONE Step은 다시 구현하지 마라. 단, 현재 OPEN Step 구현에 필요한 호환 수정은 허용한다.

문서 확인 정책:
- 우선 docs/TASKS.md의 현재 OPEN Step 섹션만 확인하라.
- PRD.md와 ARCHITECTURE.md는 구현 중 충돌이 의심되거나 세부 기준이 필요한 경우에만 관련 부분만 확인하라.
- THREADS_EXTRACTION_NOTES.md는 현재 Step이 Threads 관련 Step일 때만 확인하라.

진행 방식:
- 작업 중 나에게 설계 판단이나 구현 방식에 대해 질문하지 마라.
- 명확하지 않은 부분은 기존 코드 구조와 일반적인 구현 관례에 따라 합리적으로 결정하라.
- 프로젝트 루트 밖의 파일은 수정하지 마라.
- 기존 테스트가 통과하도록 유지하라.
- 가능하면 현재 Step 범위에 대한 테스트를 추가하라.

금지:
- 현재 OPEN Step 이후의 작업은 구현하지 마라.
- 실제 Threads 크롤링, 로컬 브라우저 세션, Chrome 실행, Playwright/Puppeteer, GraphQL 호출, RSS 수집, LLM 연동, 공식 발표 매칭, topic clustering, login/auth는 해당 Step에서 명시적으로 OPEN되지 않는 한 구현하지 마라.

검증:
- backend 변경이 있으면 GRADLE_USER_HOME=.gradle ./gradlew :backend:test 를 실행하라.
- frontend 변경이 있으면 해당 frontend 검증 명령을 실행하라.
- 검증 실패 시 가능한 범위에서 수정 후 재검증하라.

문서 상태 업데이트:
- 현재 Step 구현과 검증이 성공하면 docs/TASKS.md에서 현재 Step Status를 DONE으로 변경하라.
- 다음 Step Status가 LOCKED라면 OPEN으로 변경하라.
- 단, 다음 Step 구현은 시작하지 마라.
- 실패하면 Status를 변경하지 마라.

완료 후 보고:
1. 수행한 Step
2. 생성/수정 파일 목록
3. 구현 요약
4. 실행/검증 명령과 결과
5. 실패했던 명령과 조치
6. 의도적으로 구현하지 않은 것
7. 다음 Step에서 해야 할 일
