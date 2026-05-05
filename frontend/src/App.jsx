import { useEffect, useMemo, useState } from 'react';
import { fetchDashboardSummary, fetchInfoItems } from './api/dashboardApi';
import {
  HIDDEN_SECTION,
  PRIMARY_SECTIONS,
  groupItemsByDashboardSection,
} from './dashboardSections';

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

function App() {
  const [summary, setSummary] = useState(null);
  const [items, setItems] = useState([]);
  const [includeHidden, setIncludeHidden] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

function DashboardSection({ section, items, loading }) {
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

        {!loading && items.map((item) => <InfoItemCard key={item.id} item={item} />)}
      </div>
    </section>
  );
}

function InfoItemCard({ item }) {
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
      </div>

      <h3 className="mt-3 line-clamp-2 text-base font-semibold leading-6 text-slate-950">
        {item.title}
      </h3>
      <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">{item.summary}</p>

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

export default App;
