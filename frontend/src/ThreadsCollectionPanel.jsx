import { useEffect, useState } from 'react';
import { collectEnabledThreads, collectThreads, deleteCollectionRun, fetchCollectionRuns } from './api/dashboardApi';
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
  ACCESS_RESTRICTED: '접근 제한: 해당 소스 실패를 기록했습니다.',
  TIMEOUT: '시간 초과: 해당 소스 실패를 기록했습니다.',
  COOLDOWN_SKIPPED: '최근 수집된 Source라서 쿨다운 정책에 따라 건너뛰었습니다.',
};

export function ThreadsCollectionPanel({ onDashboardRefresh }) {
  const [accountUrl, setAccountUrl] = useState('');
  const [maxPostsPerAccount, setMaxPostsPerAccount] = useState(3);
  const [maxScrollCount, setMaxScrollCount] = useState(5);
  const [runs, setRuns] = useState([]);
  const [loadingRuns, setLoadingRuns] = useState(true);
  const [collecting, setCollecting] = useState(false);
  const [collectingEnabled, setCollectingEnabled] = useState(false);
  const [deletingRunIds, setDeletingRunIds] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const busy = collecting || collectingEnabled;
  const resultFailureReason = summarizeFailureReason(result?.failureReason);

  async function refreshRuns() {
    setLoadingRuns(true);

    try {
      const nextRuns = await fetchCollectionRuns();
      setRuns(nextRuns);
      return nextRuns;
    } finally {
      setLoadingRuns(false);
    }
  }

  async function handleDeleteRun(runId) {
    const previousRuns = runs;
    setRuns((currentRuns) => currentRuns.filter((run) => run.id !== runId));
    setDeletingRunIds((currentIds) => [...currentIds, runId]);
    setError('');

    try {
      await deleteCollectionRun(runId);
      const nextRuns = await fetchCollectionRuns();
      setRuns(nextRuns);
    } catch (nextError) {
      setRuns(previousRuns);
      setError(`수집 실행 삭제에 실패했습니다. ${nextError.message}`);
    } finally {
      setDeletingRunIds((currentIds) => currentIds.filter((id) => id !== runId));
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

  async function handleCollectEnabled() {
    setCollectingEnabled(true);
    setError('');
    setResult(null);

    try {
      const response = await collectEnabledThreads();
      setResult(response);
      await Promise.all([refreshRuns(), onDashboardRefresh?.()]);
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setCollectingEnabled(false);
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

  useEffect(() => {
    if (!busy) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      refreshRuns();
    }, 1500);

    refreshRuns();

    return () => {
      window.clearInterval(intervalId);
    };
  }, [busy]);

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
        <form className="flex flex-col gap-4" onSubmit={handleCollect}>
          <div>
            <h2 className="text-base font-semibold text-slate-950">Threads 수집</h2>
            <p className="mt-1 text-sm text-slate-600">
              운영 수집은 활성 소스 전체를 대상으로 실행합니다. 단일 URL 입력은 계정별 문제를 확인하는
              진단 용도입니다.
            </p>
          </div>

          <div className="rounded-md border border-teal-100 bg-teal-50/70 p-3">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-teal-900">활성 소스 전체 수집</p>
                <p className="mt-1 text-sm text-teal-800">
                  활성화된 Threads 소스를 우선순위대로 수집하고 최근 3일 게시물만 저장합니다. 계정별
                  실패는 기록한 뒤 다음 소스를 계속 처리합니다.
                </p>
              </div>
              <button
                className="w-fit rounded-md bg-teal-700 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
                type="button"
                disabled={busy}
                onClick={handleCollectEnabled}
              >
                {collectingEnabled ? '활성 소스 수집 중' : '활성 소스 전체 수집'}
              </button>
            </div>
          </div>

          <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
            단일 계정 진단 URL
            <input
              className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              type="url"
              value={accountUrl}
              placeholder="https://www.threads.com/@example"
              onChange={(event) => setAccountUrl(event.target.value)}
            />
          </label>

          <label className="flex max-w-xs flex-col gap-1.5 text-sm font-medium text-slate-700">
            계정당 최대 게시물
            <input
              className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              type="number"
              min="1"
              max="100"
              value={maxPostsPerAccount}
              onChange={(event) => setMaxPostsPerAccount(event.target.value)}
            />
          </label>

          <label className="flex max-w-xs flex-col gap-1.5 text-sm font-medium text-slate-700">
            최대 스크롤
            <input
              className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              type="number"
              min="0"
              max="30"
              value={maxScrollCount}
              onChange={(event) => setMaxScrollCount(event.target.value)}
            />
          </label>

          <button
            className="w-fit rounded-md bg-teal-700 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
            type="submit"
            disabled={busy}
          >
            {collecting ? '진단 수집 중' : '단일 계정 진단 수집'}
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
                <CollectionRunRow
                  key={run.id}
                  run={run}
                  deleting={deletingRunIds.includes(run.id)}
                  onDelete={handleDeleteRun}
                />
              ))}
          </div>
        </div>
      </div>

      {result ? (
        <StateMessage tone={result.failedCount > 0 ? 'warning' : 'success'} className="mt-4">
          실행 결과: 수집 {result.collectedCount}, 생성 {result.createdCount}, 중복{' '}
          {result.duplicateCount}, 실패 {result.failedCount}
          {resultFailureReason ? (
            <span className="mt-2 block whitespace-pre-wrap break-words text-red-800">
              실패 사유: {resultFailureReason}
            </span>
          ) : null}
          <SafetyResultMessage result={result} />
          <SourceResults sourceResults={result.sourceResults} />
        </StateMessage>
      ) : null}

      {busy ? (
        <StateMessage tone="loading" className="mt-4">
          {collectingEnabled ? '활성 소스 전체 수집을 진행 중입니다.' : '단일 계정 진단 수집을 진행 중입니다.'}
        </StateMessage>
      ) : null}

      {error ? (
        <StateMessage tone="error" className="mt-4">
          수집 API를 호출하지 못했습니다. {error}
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
      ? `적용 제한: 계정당 게시물 ${result.appliedMaxPostsPerAccount}/${result.requestedMaxPostsPerAccount}`
      : '';

  return (
    <>
      {appliedMessage ? <span className="mt-2 block">{appliedMessage}</span> : null}
      {result.safetyMessage ? <span className="mt-1 block">{result.safetyMessage}</span> : null}
      {explicitMessage ? <span className="mt-1 block">{explicitMessage}</span> : null}
    </>
  );
}

function SourceResults({ sourceResults = [] }) {
  if (sourceResults.length === 0) {
    return null;
  }

  return (
    <span className="mt-3 block border-t border-slate-200 pt-2">
      {sourceResults.map((sourceResult) => (
        <span className="block" key={sourceResult.id ?? sourceResult.sourceId}>
          {sourceResult.sourceName}: {sourceResult.status} · 생성 {sourceResult.createdCount} · 중복{' '}
          {sourceResult.duplicateCount} · 실패 {sourceResult.failedCount}
        </span>
      ))}
    </span>
  );
}

function CollectionRunRow({ run, deleting, onDelete }) {
  const [expanded, setExpanded] = useState(false);
  const statusLabel = runStatusLabels[run.status] ?? run.status;
  const statusTone = runStatusTones[run.status] ?? runStatusTones.FAILED;
  const failureReason = summarizeFailureReason(run.failureReason);
  const statusMessage = summarizeStatusMessage(run.statusMessage);

  async function handleDelete() {
    await onDelete?.(run.id);
  }

  return (
    <article className="rounded-md border border-slate-200 px-3 py-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <button
          className="flex flex-wrap items-center gap-2 text-left"
          type="button"
          aria-expanded={expanded}
          aria-label={`${statusLabel} 실행 #${run.id} 상세 ${expanded ? '접기' : '펼치기'}`}
          onClick={() => setExpanded((current) => !current)}
        >
          <StatusBadge tone={statusTone}>{statusLabel}</StatusBadge>
          <span className="text-xs text-slate-500">실행 #{run.id}</span>
        </button>
        <button
          className="rounded-md border border-slate-300 px-2.5 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          type="button"
          disabled={deleting}
          onClick={handleDelete}
        >
          {deleting ? '삭제 중' : '삭제'}
        </button>
      </div>
      {expanded ? (
        <>
          <p className="mt-2 text-sm text-slate-700">
            수집 {run.collectedItemCount}, 생성 {run.createdCount}, 중복 {run.duplicateCount}, 실패{' '}
            {run.failedCount}
          </p>
          {statusMessage ? (
            <p className="mt-1 whitespace-pre-wrap break-words text-sm text-slate-600">{statusMessage}</p>
          ) : null}
          <p className="mt-1 text-xs text-slate-500">
            생성 {formatDateTime(run.createdAt)} · 갱신 {formatDateTime(run.updatedAt)}
          </p>
          {failureReason ? (
            <p className="mt-2 whitespace-pre-wrap break-words rounded-md border border-red-100 bg-red-50 px-2.5 py-2 text-sm text-red-700">
              실패 사유: {failureReason}
            </p>
          ) : null}
          <SourceResults sourceResults={run.sourceResults} />
        </>
      ) : null}
    </article>
  );
}

function summarizeFailureReason(failureReason) {
  if (!failureReason) {
    return failureReason;
  }

  const compactReason = failureReason.replace(/\s+/g, ' ').trim();
  if (compactReason.length <= 220) {
    return compactReason;
  }
  return `${compactReason.slice(0, 220)}...`;
}

function summarizeStatusMessage(statusMessage) {
  if (!statusMessage) {
    return statusMessage;
  }

  const compactMessage = statusMessage.replace(/\s+/g, ' ').trim();
  if (compactMessage.length <= 220) {
    return compactMessage;
  }
  return `${compactMessage.slice(0, 220)}...`;
}
