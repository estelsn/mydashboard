import { useEffect, useState } from 'react';
import { fetchThreadsSessionStatus, openThreadsLoginBrowser } from './api/dashboardApi';

const sessionStatusLabels = {
  NOT_CONFIGURED: '설정 필요',
  LOGIN_REQUIRED: '로그인 필요',
  READY: '준비됨',
  EXPIRED: '만료됨',
  ERROR: '오류',
};

const sessionStatusStyles = {
  NOT_CONFIGURED: 'border-slate-200 bg-slate-100 text-slate-700',
  LOGIN_REQUIRED: 'border-amber-200 bg-amber-50 text-amber-800',
  READY: 'border-teal-200 bg-teal-50 text-teal-800',
  EXPIRED: 'border-orange-200 bg-orange-50 text-orange-800',
  ERROR: 'border-red-200 bg-red-50 text-red-800',
};

export function ThreadsSessionPanel() {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [opening, setOpening] = useState(false);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  async function refreshSessionStatus() {
    setLoading(true);
    setError('');

    try {
      const nextSession = await fetchThreadsSessionStatus();
      setSession(nextSession);
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleOpenLoginBrowser() {
    setOpening(true);
    setError('');
    setActionMessage('');

    try {
      const response = await openThreadsLoginBrowser();
      setActionMessage(response.message ?? '로그인 브라우저를 열었습니다.');
      await refreshSessionStatus();
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setOpening(false);
    }
  }

  useEffect(() => {
    let ignore = false;

    async function loadSessionStatus() {
      setLoading(true);
      setError('');

      try {
        const nextSession = await fetchThreadsSessionStatus();

        if (!ignore) {
          setSession(nextSession);
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

    loadSessionStatus();

    return () => {
      ignore = true;
    };
  }, []);

  const status = session?.status ?? 'LOGIN_REQUIRED';
  const statusLabel = sessionStatusLabels[status] ?? status;
  const statusClassName = sessionStatusStyles[status] ?? sessionStatusStyles.ERROR;
  const busy = loading || opening;

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-base font-semibold text-slate-950">Threads 세션</h2>
            <span
              className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${statusClassName}`}
              data-testid="threads-session-status"
            >
              {loading && !session ? '확인 중' : statusLabel}
            </span>
          </div>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            {session?.message ?? 'Threads 로그인 세션 상태를 확인합니다.'}
          </p>
          {session?.profilePath ? (
            <p className="mt-1 break-all text-xs text-slate-500">프로필: {session.profilePath}</p>
          ) : null}
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            disabled={busy}
            onClick={refreshSessionStatus}
          >
            {loading ? '확인 중' : '세션 상태 다시 확인'}
          </button>
          <button
            className="rounded-md bg-teal-700 px-3 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            disabled={busy}
            onClick={handleOpenLoginBrowser}
          >
            {opening ? '여는 중' : '로그인 브라우저 열기'}
          </button>
        </div>
      </div>

      {actionMessage ? (
        <p className="mt-3 rounded-md border border-teal-200 bg-teal-50 px-3 py-2 text-sm text-teal-800">
          {actionMessage}
        </p>
      ) : null}

      {error ? (
        <p className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          Threads 세션 API를 호출하지 못했습니다. {error}
        </p>
      ) : null}
    </section>
  );
}
