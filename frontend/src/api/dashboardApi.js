const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

async function requestJson(path) {
  const response = await fetch(`${API_BASE_URL}${path}`);

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
