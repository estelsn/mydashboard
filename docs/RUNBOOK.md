# Local Runbook

## Prerequisites

- Java 21
- Node.js 20+
- npm

## Backend

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:bootRun
```

Backend API base URL:

```text
http://localhost:8080
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

The Vite dev server proxies `/api` requests to `http://localhost:8080`.

## Verification

Backend:

```bash
GRADLE_USER_HOME=.gradle ./gradlew :backend:test
```

Frontend:

```bash
cd frontend
npm test
npm run build
```
