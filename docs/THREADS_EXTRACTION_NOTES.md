# Threads Extraction Notes

## 1. 문서 목적

이 문서는 향후 Threads 수집기를 만들 때 참고하기 위한 실험 기록이다.

현재 Step 1~6에서는 이 문서의 내용을 구현하지 않는다.

금지 사항:

- Step 1~6에서 Chrome 실행 코드 작성 금지
- Step 1~6에서 Playwright/Puppeteer 추가 금지
- Step 1~6에서 curl 기반 Threads 호출 구현 금지
- Step 1~6에서 GraphQL 직접 호출 구현 금지
- Step 1~6에서 실제 Threads 수집 버튼 구현 금지

이 문서는 구현 지시가 아니라 참고 자료다.

## 2. 대상 예시

```text
https://www.threads.com/@choi.openai
```

## 3. 핵심 결론

curl로 받은 초기 HTML에는 프로필 메타 정보만 있고 포스트 본문은 거의 없다.

Threads는 클라이언트 렌더링, SSR, GraphQL preload 조합으로 동작한다.

실험 결과 가장 안정적으로 성공한 방식은 headless Chrome으로 렌더링된 DOM을 저장한 뒤 텍스트를 정제하는 방식이다.

## 4. 공개 페이지 HTML 확인

```bash
curl -L -s -A 'Mozilla/5.0' \
  'https://www.threads.com/@choi.openai' \
  -o /tmp/threads.html
```

초기 HTML에서 확인 가능했던 것:

- 프로필명
- 소개문
- 팔로워 수
- 프로필 이미지
- 라우트 정보
- GraphQL operation/doc_id 일부

하지만 포스트 본문은 안정적으로 나오지 않았다.

## 5. JS/GraphQL 정보 탐색

HTML/번들에서 다음 값을 찾았다.

```text
user_id: 63452224831
operation: BarcelonaProfileThreadsTabDirectQuery
doc_id: 26738829505750294
```

사용한 패턴:

```bash
rg 'user_id|BarcelonaProfileThreadsTabDirectQuery|doc_id|RelayPreloader' /tmp/threads.html
rg 'BarcelonaProfileThreadsTabDirectQuery' /tmp/threads_bundle_*.js
```

## 6. GraphQL 직접 호출 시도

```bash
curl -L -s 'https://www.threads.com/api/graphql' \
  -H 'content-type: application/x-www-form-urlencoded' \
  -H 'x-fb-lsd: ...' \
  -H 'x-asbd-id: 359341' \
  -H 'user-agent: Mozilla/5.0' \
  -H 'origin: https://www.threads.com' \
  -H 'referer: https://www.threads.com/@choi.openai' \
  --data-urlencode 'fb_api_req_friendly_name=BarcelonaProfileThreadsTabDirectQuery' \
  --data-urlencode 'variables={...}' \
  --data-urlencode 'doc_id=26738829505750294'
```

결과:

```json
{"data":{"mediaData":null,"viewer":null},"extensions":{"is_final":true}}
```

로그인 없는 단순 API 호출은 성공 응답이어도 실제 포스트 데이터를 주지 않았다.

## 7. 성공한 최종 방법: Chrome headless 렌더링

```bash
/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome \
  --headless=new \
  --disable-gpu \
  --no-sandbox \
  --user-data-dir=/private/tmp/chrome-threads-profile \
  --virtual-time-budget=10000 \
  --dump-dom \
  'https://www.threads.com/@choi.openai' \
  > /private/tmp/threads_dom_rendered.html
```

핵심 옵션:

- --headless=new: 실제 브라우저 렌더링
- --virtual-time-budget=10000: JS/SSR/비동기 로딩 대기
- --dump-dom: 렌더링 후 DOM 출력
- --user-data-dir=...: 임시 브라우저 프로필 격리

## 8. DOM 텍스트 정제

```bash
node -e "
const fs=require('fs');
let h=fs.readFileSync('/private/tmp/threads_dom_rendered.html','utf8');

h=h
  .replace(/<script[\s\S]*?<\/script>/g,' ')
  .replace(/<style[\s\S]*?<\/style>/g,' ')
  .replace(/<[^>]+>/g,'\n')
  .replace(/&amp;/g,'&')
  .replace(/&lt;/g,'<')
  .replace(/&gt;/g,'>')
  .replace(/&quot;/g,'\"')
  .replace(/&#x27;/g,"'")
  .replace(/&#(\d+);/g,(m,n)=>String.fromCharCode(+n))
  .replace(/&#x([0-9a-fA-F]+);/g,(m,n)=>String.fromCharCode(parseInt(n,16)));

const lines=h
  .split(/\n+/)
  .map(s=>s.trim())
  .filter(s=>s && !s.startsWith('.') && !s.includes('{--') && s.length < 1000);

const uniq=[];
for (const s of lines) {
  if (!uniq.includes(s)) uniq.push(s);
}

console.log(uniq.join('\n'));
"
```

## 9. 추출 가능했던 내용

로그인 없이 공개로 노출되는 범위에서 다음을 추출할 수 있었다.

- 프로필 정보
- 탭 이름
- 고정 포스트
- 최근 포스트 일부
- 포스트 본문
- 좋아요/댓글/리포스트/공유 수
- 링크 카드 제목
- 로그인 유도 문구

일정 수 이후에는 다음 문구가 나오며 제한된다.

```text
로그인하여 choi.openai님의 소식을 더 확인해보세요.
```

## 10. 중요한 주의점

- 비밀번호를 직접 받지 말 것.
- 로그인 범위까지 필요하면 사용자가 직접 로컬 Chrome에서 로그인한 세션을 쓰는 방식이 안전하다.
- 쿠키/세션 export는 민감 정보라 비추천한다.
- Threads는 구조가 자주 바뀌므로 doc_id, GraphQL variables, DOM 클래스에 의존하는 코드는 깨질 수 있다.
- 가장 안정적인 방식은 브라우저 렌더링 후 텍스트 추출이다.
- 대량 수집은 Threads 이용약관, 속도 제한, 계정 제한에 걸릴 수 있다.

## 11. 향후 구현 방향

향후 수집기를 구현할 경우 다음 파이프라인을 고려한다.

1. URL 입력
2. headless Chrome으로 렌더링 DOM 저장
3. script/style 제거
4. HTML 태그 제거
5. entity decode
6. 중복 줄 제거
7. 포스트 경계 추정
8. JSON/CSV/Markdown으로 저장

포스트 경계 추정 기준:

- 고정됨
- 3시간, 1시간, 6시간, 1분, 어제 같은 시간 표현
- 좋아요, 댓글, 리포스트, 공유하기
- 계정명/본문/링크 카드 반복 구조

## 12. 로컬 Chrome 프로세스 정리

작업 후 headless Chrome이 남아 있으면 정리한다.

```bash
pkill -f 'chrome-threads-profile'
```
