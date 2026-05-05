const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: options.body ? { 'Content-Type': 'application/json' } : undefined,
    ...options,
  });

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status}`);
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
