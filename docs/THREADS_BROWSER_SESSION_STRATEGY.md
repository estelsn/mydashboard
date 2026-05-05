# Threads Browser Session Strategy

## Goal

Threads collection uses an app-owned Chrome profile directory instead of storing credentials, cookies, session tokens, or authentication codes.

## Profile Directory

Default profile directory:

```text
./runtime/browser-profiles/threads
```

The `runtime/` directory is local runtime data and is excluded from Git tracking.

## Session Handling

- The app never stores Threads ID, password, authentication code, cookie strings, or session tokens.
- The user signs in to Threads directly in Chrome opened with the app profile directory.
- Later collectors reuse the same profile directory to access Threads with the local browser session.
- This step only models the session boundary; it does not launch Chrome, connect to Threads, or collect posts.

## Status Values

- `NOT_CONFIGURED`: profile directory setting is absent.
- `LOGIN_REQUIRED`: profile directory does not exist yet, so the user has not initialized the app profile.
- `READY`: profile directory exists and can be read by the app.
- `EXPIRED`: reserved for a later step that can detect an expired Threads session.
- `ERROR`: profile path is invalid or inaccessible.
