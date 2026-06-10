import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  collectEnabledThreads,
  deleteCollectionRun,
  fetchThreadsSessionStatus,
  openThreadsLoginBrowser,
} from './dashboardApi';

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

  it('handles a successful collection run delete with an empty 204 response', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      headers: {
        get: vi.fn().mockReturnValue(null),
      },
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(deleteCollectionRun(7)).resolves.toBeNull();
    expect(fetchMock).toHaveBeenCalledWith('/api/collection-runs/7', {
      headers: undefined,
      cache: undefined,
      method: 'DELETE',
    });
  });

  it('collects all enabled Threads sources through the enabled endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: {
        get: vi.fn().mockReturnValue('application/json'),
      },
      json: vi.fn().mockResolvedValue({ runId: 8 }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(collectEnabledThreads()).resolves.toEqual({ runId: 8 });
    expect(fetchMock).toHaveBeenCalledWith('/api/collection-runs/threads/enabled', {
      headers: undefined,
      cache: undefined,
      method: 'POST',
    });
  });
});
