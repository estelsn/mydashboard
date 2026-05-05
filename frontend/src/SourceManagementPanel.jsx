import { useEffect, useState } from 'react';
import { fetchSources, updateSourceEnabled } from './api/dashboardApi';

const sourceTypeLabels = {
  THREADS_ACCOUNT: 'Threads',
  RSS_FEED: 'RSS',
  OFFICIAL_SITE: '공식 사이트',
};

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function SourceManagementPanel() {
  const [sources, setSources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pendingSourceId, setPendingSourceId] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    async function loadSources() {
      setLoading(true);
      setError('');

      try {
        const nextSources = await fetchSources();

        if (!ignore) {
          setSources(nextSources);
        }
      } catch (nextError) {
        if (!ignore) {
          setError(nextError.message);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadSources();

    return () => {
      ignore = true;
    };
  }, []);

  async function handleEnabledChange(source, enabled) {
    setPendingSourceId(source.id);
    setError('');

    try {
      const updatedSource = await updateSourceEnabled(source.id, enabled);
      setSources((currentSources) =>
        currentSources.map((currentSource) =>
          currentSource.id === updatedSource.id ? updatedSource : currentSource,
        ),
      );
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setPendingSourceId(null);
    }
  }

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-slate-950">수집 Source</h2>
        <span className="text-sm font-medium text-slate-500">총 {sources.length}</span>
      </div>

      {loading ? (
        <p className="mt-4 rounded-md border border-slate-200 bg-slate-50 px-3 py-4 text-sm text-slate-500">
          Source 목록 불러오는 중
        </p>
      ) : null}

      {!loading && sources.length === 0 ? (
        <p className="mt-4 rounded-md border border-slate-200 bg-slate-50 px-3 py-4 text-sm text-slate-500">
          등록된 Source 없음
        </p>
      ) : null}

      {!loading && sources.length > 0 ? (
        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full table-fixed divide-y divide-slate-200 text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="w-56 px-3 py-2">name</th>
                <th className="w-32 px-3 py-2">sourceType</th>
                <th className="min-w-72 px-3 py-2">url</th>
                <th className="w-32 px-3 py-2">enabled</th>
                <th className="w-24 px-3 py-2">priority</th>
                <th className="w-40 px-3 py-2">lastCollectedAt</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {sources.map((source) => (
                <SourceRow
                  key={source.id}
                  source={source}
                  pending={pendingSourceId === source.id}
                  onEnabledChange={handleEnabledChange}
                />
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {error ? (
        <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          Source API를 호출하지 못했습니다. {error}
        </p>
      ) : null}
    </section>
  );
}

function SourceRow({ source, pending, onEnabledChange }) {
  const enabledLabel = source.enabled ? '활성' : '비활성';

  return (
    <tr>
      <td className="px-3 py-3 align-top font-medium text-slate-950">{source.name}</td>
      <td className="px-3 py-3 align-top text-slate-700">
        {sourceTypeLabels[source.sourceType] ?? source.sourceType}
      </td>
      <td className="px-3 py-3 align-top">
        <a
          className="break-all font-medium text-teal-700 hover:text-teal-900"
          href={source.url}
          target="_blank"
          rel="noreferrer"
        >
          {source.url}
        </a>
      </td>
      <td className="px-3 py-3 align-top">
        <label className="flex w-fit items-center gap-2 text-xs font-semibold text-slate-700">
          <input
            className="h-4 w-4 accent-teal-700 disabled:cursor-not-allowed"
            type="checkbox"
            checked={source.enabled}
            disabled={pending}
            aria-label={`${source.name} enabled`}
            onChange={(event) => onEnabledChange(source, event.target.checked)}
          />
          <span
            className={`rounded-full px-2.5 py-1 ${
              source.enabled ? 'bg-teal-50 text-teal-800' : 'bg-slate-100 text-slate-600'
            }`}
          >
            {pending ? '저장 중' : enabledLabel}
          </span>
        </label>
      </td>
      <td className="px-3 py-3 align-top text-slate-700">{source.priority}</td>
      <td className="px-3 py-3 align-top text-slate-600">{formatDateTime(source.lastCollectedAt)}</td>
    </tr>
  );
}
