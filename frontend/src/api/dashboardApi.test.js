import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchThreadsSessionStatus, openThreadsLoginBrowser } from './dashboardApi';

describe('dashboardApi', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('includes the API path and method when an empty error response fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: vi.fn().mockRejectedValue(new SyntaxError('empty body')),
      }),
    );

    await expect(openThreadsLoginBrowser()).rejects.toThrow(
      'POST /api/threads/session/open-login failed with HTTP 403',
    );
  });

  it('includes problem detail when an error response has a body', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        json: vi.fn().mockResolvedValue({
          detail: 'Failed to open Chrome for Threads login',
        }),
      }),
    );

    await expect(fetchThreadsSessionStatus()).rejects.toThrow(
      'GET /api/threads/session failed with HTTP 500 - Failed to open Chrome for Threads login',
    );
  });
});
