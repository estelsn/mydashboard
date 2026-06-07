const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

async function requestJson(path, options = {}) {
  const method = options.method ?? 'GET';
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: options.body ? { 'Content-Type': 'application/json' } : undefined,
    cache: method === 'GET' ? 'no-store' : options.cache,
    ...options,
  });

  if (!response.ok) {
    let detail = '';

    try {
      const payload = await response.json();
      detail = payload.detail ?? payload.message ?? payload.title ?? '';
    } catch {
      detail = '';
    }

    const suffix = detail ? ` - ${detail}` : '';
    throw new Error(`${method} ${path} failed with HTTP ${response.status}${suffix}`);
  }

  if (response.status === 204) {
    return null;
  }

  const contentLength = response.headers.get('content-length');
  if (contentLength === '0') {
    return null;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    return null;
  }

  return response.json();
}

export function fetchDashboardSummary() {
  return requestJson('/api/dashboard/summary');
}

export function fetchInfoItems({ includeHidden }) {
  return requestJson(`/api/info-items?includeHidden=${includeHidden}`);
}

export function updateInfoItemDecisionStatus(id, decisionStatus) {
  return requestJson(`/api/info-items/${id}/decision-status`, {
    method: 'PATCH',
    body: JSON.stringify({ decisionStatus }),
  });
}

export function archiveInfoItem(id) {
  return requestJson(`/api/info-items/${id}/archive`, {
    method: 'PATCH',
  });
}

export function restoreInfoItem(id) {
  return requestJson(`/api/info-items/${id}/restore`, {
    method: 'PATCH',
  });
}

export function fetchThreadsSessionStatus() {
  return requestJson('/api/threads/session');
}

export function openThreadsLoginBrowser() {
  return requestJson('/api/threads/session/open-login', {
    method: 'POST',
  });
}

export function fetchCollectionRuns() {
  return requestJson('/api/collection-runs');
}

export function deleteCollectionRun(id) {
  return requestJson(`/api/collection-runs/${id}`, {
    method: 'DELETE',
  });
}

export function collectThreads({ accountUrls, maxPostsPerAccount, maxScrollCount }) {
  return requestJson('/api/collection-runs/threads', {
    method: 'POST',
    body: JSON.stringify({
      accountUrls,
      maxPostsPerAccount,
      maxScrollCount,
    }),
  });
}

export function collectRecentThreads() {
  return requestJson('/api/collection-runs/threads/recent', {
    method: 'POST',
  });
}

export function fetchSources() {
  return requestJson('/api/sources');
}

export function updateSourceEnabled(id, enabled) {
  return requestJson(`/api/sources/${id}/enabled`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled }),
  });
}
