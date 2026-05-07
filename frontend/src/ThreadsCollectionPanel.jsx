import { useEffect, useState } from 'react';
import { collectThreads, fetchCollectionRuns } from './api/dashboardApi';
import { StateMessage, StatusBadge, formatDateTime } from './ui';

const runStatusLabels = {
  RUNNING: '실행 중',
  SUCCEEDED: '성공',
  FAILED: '실패',
  PARTIAL_SUCCESS: '부분 성공',
};

const runStatusTones = {
  RUNNING: 'sky',
  SUCCEEDED: 'teal',
  FAILED: 'red',
  PARTIAL_SUCCESS: 'amber',
};

const safetyStatusMessages = {
  LOGIN_REQUIRED: '세션 확인 필요: Threads 로그인이 필요합니다.',
  ACCESS_RESTRICTED: '접근 제한 가능성: 수집을 즉시 중단했습니다.',
  TIMEOUT: '시간 초과: 수집을 즉시 중단했습니다.',
  COOLDOWN_SKIPPED: '최근 수집된 Source라서 쿨다운 정책에 따라 건너뛰었습니다.',
};

export function ThreadsCollectionPanel({ onDashboardRefresh }) {
  const [accountUrl, setAccountUrl] = useState('');
  const [maxPostsPerAccount, setMaxPostsPerAccount] = useState(3);
  const [maxScrollCount, setMaxScrollCount] = useState(1);
  const [runs, setRuns] = useState([]);
  const [loadingRuns, setLoadingRuns] = useState(true);
  const [collecting, setCollecting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  async function refreshRuns() {
    setLoadingRuns(true);

    try {
      const nextRuns = await fetchCollectionRuns();
      setRuns(nextRuns);
    } finally {
      setLoadingRuns(false);
    }
  }

  async function handleCollect(event) {
    event.preventDefault();

    const trimmedUrl = accountUrl.trim();
    if (!trimmedUrl) {
      setError('수집 대상 Threads 계정 URL을 입력하세요.');
      setResult(null);
      return;
    }

    setCollecting(true);
    setError('');
    setResult(null);

    try {
      const response = await collectThreads({
        accountUrls: [trimmedUrl],
        maxPostsPerAccount: Number(maxPostsPerAccount),
        maxScrollCount: Number(maxScrollCount),
      });

      setResult(response);
      await Promise.all([refreshRuns(), onDashboardRefresh?.()]);
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setCollecting(false);
    }
  }

  useEffect(() => {
    let ignore = false;

    async function loadRuns() {
      setLoadingRuns(true);
      setError('');

      try {
        const nextRuns = await fetchCollectionRuns();

        if (!ignore) {
          setRuns(nextRuns);
        }
      } catch (nextError) {
        if (!ignore) {
          setError(nextError.message);
        }
      } finally {
        if (!ignore) {
          setLoadingRuns(false);
        }
      }
    }

    loadRuns();

    return () => {
      ignore = true;
    };
  }, []);

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
        <form className="flex flex-col gap-4" onSubmit={handleCollect}>
          <div>
            <h2 className="text-base font-semibold text-slate-950">Threads 수동 수집</h2>
            <p className="mt-1 text-sm text-slate-600">
              Threads 수집은 계정 보호를 위해 낮은 기본값과 대기 시간을 적용합니다. 로그인 필요,
              접근 제한, 시간 초과가 감지되면 수집을 즉시 중단합니다.
            </p>
          </div>

          <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
            계정 URL
            <input
              className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              type="url"
              value={accountUrl}
              placeholder="https://www.threads.com/@example"
              onChange={(event) => setAccountUrl(event.target.value)}
            />
          </label>

          <div className="grid gap-3 sm:grid-cols-2">
            <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
              계정당 최대 게시물
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                type="number"
                min="1"
                max="5"
                value={maxPostsPerAccount}
                onChange={(event) => setMaxPostsPerAccount(event.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
              최대 스크롤
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                type="number"
                min="0"
                max="2"
                value={maxScrollCount}
                onChange={(event) => setMaxScrollCount(event.target.value)}
              />
            </label>
          </div>

          <button
            className="w-fit rounded-md bg-teal-700 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
            type="submit"
            disabled={collecting}
          >
            {collecting ? '수집 실행 중' : '수집 실행'}
          </button>
        </form>

        <div>
          <h3 className="text-sm font-semibold text-slate-950">최근 수집 실행</h3>
          <div className="mt-3 flex flex-col gap-2">
            {loadingRuns ? (
              <StateMessage tone="loading">
                수집 실행 목록 불러오는 중
              </StateMessage>
            ) : null}

            {!loadingRuns && runs.length === 0 ? (
              <StateMessage tone="empty">
                수집 실행 기록 없음
              </StateMessage>
            ) : null}

            {!loadingRuns &&
              runs.map((run) => (
                <CollectionRunRow key={run.id} run={run} />
              ))}
          </div>
        </div>
      </div>

      {result ? (
        <StateMessage tone={result.failedCount > 0 ? 'warning' : 'success'} className="mt-4">
          실행 결과: 수집 {result.collectedCount}, 생성 {result.createdCount}, 중복{' '}
          {result.duplicateCount}, 실패 {result.failedCount}
          {result.failureReason ? (
            <span className="mt-2 block text-red-800">실패 사유: {result.failureReason}</span>
          ) : null}
          <SafetyResultMessage result={result} />
        </StateMessage>
      ) : null}

      {error ? (
        <StateMessage tone="error" className="mt-4">
          Threads 수집 API를 호출하지 못했습니다. {error}
        </StateMessage>
      ) : null}
    </section>
  );
}

function SafetyResultMessage({ result }) {
  const explicitMessage = Object.entries(safetyStatusMessages).find(([status]) =>
    [result.failureReason, result.safetyMessage].some((value) => value?.includes(status)),
  )?.[1];
  const appliedMessage =
    result.requestedMaxPostsPerAccount && result.appliedMaxPostsPerAccount
      ? `적용 제한: 계정당 게시물 ${result.appliedMaxPostsPerAccount}/${result.requestedMaxPostsPerAccount}, 스크롤 ${result.appliedMaxScrollCount}/${result.requestedMaxScrollCount}`
      : '';

  return (
    <>
      {appliedMessage ? <span className="mt-2 block">{appliedMessage}</span> : null}
      {result.safetyMessage ? <span className="mt-1 block">{result.safetyMessage}</span> : null}
      {explicitMessage ? <span className="mt-1 block">{explicitMessage}</span> : null}
    </>
  );
}

function CollectionRunRow({ run }) {
  const statusLabel = runStatusLabels[run.status] ?? run.status;
  const statusTone = runStatusTones[run.status] ?? runStatusTones.FAILED;

  return (
    <article className="rounded-md border border-slate-200 px-3 py-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <StatusBadge tone={statusTone}>{statusLabel}</StatusBadge>
        <span className="text-xs text-slate-500">Run #{run.id}</span>
      </div>
      <p className="mt-2 text-sm text-slate-700">
        수집 {run.collectedItemCount}, 생성 {run.createdCount}, 중복 {run.duplicateCount}, 실패{' '}
        {run.failedCount}
      </p>
      <p className="mt-1 text-xs text-slate-500">
        생성 {formatDateTime(run.createdAt)} · 갱신 {formatDateTime(run.updatedAt)}
      </p>
      {run.failureReason ? (
        <p className="mt-2 rounded-md border border-red-100 bg-red-50 px-2.5 py-2 text-sm text-red-700">
          실패 사유: {run.failureReason}
        </p>
      ) : null}
    </article>
  );
}
