import { useEffect, useMemo, useState } from 'react';
import {
  archiveInfoItem,
  fetchDashboardSummary,
  fetchInfoItems,
  restoreInfoItem,
  updateInfoItemDecisionStatus,
} from './api/dashboardApi';
import {
  HIDDEN_SECTION,
  PRIMARY_SECTIONS,
  applyInfoItemUpdate,
  groupItemsByDashboardSection,
  isHiddenDashboardItem,
} from './dashboardSections';
import { ThreadsSessionPanel } from './ThreadsSessionPanel';

const summaryCards = [
  { key: 'visibleCount', label: '기본 노출' },
  { key: 'applyCount', label: '오늘 볼 것' },
  { key: 'holdCount', label: '나중에 볼 것' },
  { key: 'unreviewedCount', label: '검토 필요' },
  { key: 'hiddenCount', label: '숨김' },
];

const decisionLabels = {
  APPLY: '오늘 볼 것',
  HOLD: '나중에 볼 것',
  UNREVIEWED: '검토 필요',
  IGNORE: '숨김',
  ARCHIVE_CANDIDATE: '아카이브 후보',
};

const importanceLabels = {
  HIGH: '높음',
  MEDIUM: '중간',
  LOW: '낮음',
};

const decisionActions = [
  { status: 'APPLY', label: '오늘' },
  { status: 'HOLD', label: '나중' },
  { status: 'UNREVIEWED', label: '검토' },
];

function App() {
  const [summary, setSummary] = useState(null);
  const [items, setItems] = useState([]);
  const [includeHidden, setIncludeHidden] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pendingItemId, setPendingItemId] = useState(null);

  useEffect(() => {
    let ignore = false;

    async function loadDashboard() {
      setLoading(true);
      setError('');

      try {
        const [nextSummary, nextItems] = await Promise.all([
          fetchDashboardSummary(),
          fetchInfoItems({ includeHidden }),
        ]);

        if (!ignore) {
          setSummary(nextSummary);
          setItems(nextItems);
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

    loadDashboard();

    return () => {
      ignore = true;
    };
  }, [includeHidden]);

  const groups = useMemo(
    () => groupItemsByDashboardSection(items, includeHidden),
    [items, includeHidden],
  );

  const sections = includeHidden ? [...PRIMARY_SECTIONS, HIDDEN_SECTION] : PRIMARY_SECTIONS;

  async function handleItemUpdate(action) {
    setPendingItemId(action.id);
    setError('');

    try {
      const updatedItem = await action.run();
      const nextSummary = await fetchDashboardSummary();
      setItems((currentItems) => applyInfoItemUpdate(currentItems, updatedItem, includeHidden));
      setSummary(nextSummary);
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setPendingItemId(null);
    }
  }

  return (
    <main className="min-h-screen bg-slate-50">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
        <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-wide text-teal-700">
              AI FOMO Dashboard
            </p>
            <h1 className="mt-2 text-3xl font-semibold text-slate-950">정보 수집·필터링</h1>
          </div>

          <label className="flex w-fit cursor-pointer items-center gap-3 rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 shadow-sm">
            <input
              className="h-4 w-4 accent-teal-700"
              type="checkbox"
              checked={includeHidden}
              onChange={(event) => setIncludeHidden(event.target.checked)}
            />
            숨김 항목 보기
          </label>
        </header>

        <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {summaryCards.map((card) => (
            <SummaryCard
              key={card.key}
              label={card.label}
              value={summary?.[card.key] ?? 0}
              loading={loading && !summary}
            />
          ))}
        </section>

        <ThreadsSessionPanel />

        {error ? (
          <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            백엔드 API를 불러오지 못했습니다. {error}
          </div>
        ) : null}

        <div className="grid gap-5 xl:grid-cols-3">
          {sections.map((section) => (
            <DashboardSection
              key={section.key}
              section={section}
              items={groups[section.key] ?? []}
              loading={loading}
              pendingItemId={pendingItemId}
              onUpdateItem={handleItemUpdate}
            />
          ))}
        </div>
      </div>
    </main>
  );
}

function SummaryCard({ label, value, loading }) {
  return (
    <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-slate-950">{loading ? '-' : value}</p>
    </article>
  );
}

function DashboardSection({ section, items, loading, pendingItemId, onUpdateItem }) {
  return (
    <section className="flex min-h-80 flex-col rounded-md border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h2 className="text-base font-semibold text-slate-950">{section.title}</h2>
        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
          {items.length}
        </span>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-3">
        {loading ? (
          <p className="rounded-md border border-slate-200 bg-slate-50 px-3 py-4 text-sm text-slate-500">
            불러오는 중
          </p>
        ) : null}

        {!loading && items.length === 0 ? (
          <p className="rounded-md border border-slate-200 bg-slate-50 px-3 py-4 text-sm text-slate-500">
            {section.emptyText}
          </p>
        ) : null}

        {!loading &&
          items.map((item) => (
            <InfoItemCard
              key={item.id}
              item={item}
              pending={pendingItemId === item.id}
              onUpdateItem={onUpdateItem}
            />
          ))}
      </div>
    </section>
  );
}

function InfoItemCard({ item, pending, onUpdateItem }) {
  const latestEvaluation = item.latestEvaluation;
  const hidden = isHiddenDashboardItem(item);

  return (
    <article className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-center gap-2 text-xs font-semibold">
        <span className="rounded-full bg-teal-50 px-2.5 py-1 text-teal-800">
          {decisionLabels[item.decisionStatus] ?? item.decisionStatus}
        </span>
        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-slate-700">
          {item.category}
        </span>
        <span className="rounded-full bg-amber-50 px-2.5 py-1 text-amber-800">
          중요도 {importanceLabels[item.importanceLevel] ?? item.importanceLevel}
        </span>
        {item.manualOverride ? (
          <span className="rounded-full bg-indigo-50 px-2.5 py-1 text-indigo-800">
            수동 반영
          </span>
        ) : null}
      </div>

      <h3 className="mt-3 line-clamp-2 text-base font-semibold leading-6 text-slate-950">
        {item.title}
      </h3>
      <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">{item.summary}</p>

      {latestEvaluation ? (
        <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 px-3 py-3">
          <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 sm:grid-cols-4">
            <EvaluationMetric label="관련" value={latestEvaluation.relevanceScore} />
            <EvaluationMetric label="실행" value={latestEvaluation.actionabilityScore} />
            <EvaluationMetric label="신규" value={latestEvaluation.noveltyScore} />
            <EvaluationMetric label="확신" value={latestEvaluation.confidence} />
          </div>
          {latestEvaluation.reason ? (
            <p className="mt-3 line-clamp-3 text-sm leading-5 text-slate-700">
              {latestEvaluation.reason}
            </p>
          ) : null}
        </div>
      ) : null}

      <div className="mt-4 flex flex-wrap gap-2">
        {decisionActions.map((action) => (
          <button
            key={action.status}
            className="rounded-md border border-slate-300 px-2.5 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            disabled={pending || item.decisionStatus === action.status}
            onClick={() =>
              onUpdateItem({
                id: item.id,
                run: () => updateInfoItemDecisionStatus(item.id, action.status),
              })
            }
          >
            {action.label}
          </button>
        ))}
        <button
          className="rounded-md border border-slate-300 px-2.5 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          type="button"
          disabled={pending}
          onClick={() =>
            onUpdateItem({
              id: item.id,
              run: () => (hidden ? restoreInfoItem(item.id) : archiveInfoItem(item.id)),
            })
          }
        >
          {hidden ? '복구' : '숨김'}
        </button>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
        <span>{item.sourceName}</span>
        <a
          className="font-medium text-teal-700 hover:text-teal-900"
          href={item.originalUrl}
          target="_blank"
          rel="noreferrer"
        >
          원문
        </a>
      </div>
    </article>
  );
}

function EvaluationMetric({ label, value }) {
  return (
    <div>
      <span className="block font-medium text-slate-500">{label}</span>
      <span className="mt-1 block text-sm font-semibold text-slate-950">
        {Number.isFinite(value) ? value.toFixed(2) : '-'}
      </span>
    </div>
  );
}

export default App;
