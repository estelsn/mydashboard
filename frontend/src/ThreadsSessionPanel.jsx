import { useEffect, useState } from 'react';
import { fetchThreadsSessionStatus, openThreadsLoginBrowser } from './api/dashboardApi';
import { StateMessage, StatusBadge } from './ui';

const sessionStatusLabels = {
  NOT_CONFIGURED: '설정 필요',
  LOGIN_REQUIRED: '로그인 필요',
  READY: '준비됨',
  EXPIRED: '만료됨',
  ERROR: '오류',
};

const sessionStatusTones = {
  NOT_CONFIGURED: 'slate',
  LOGIN_REQUIRED: 'amber',
  READY: 'teal',
  EXPIRED: 'orange',
  ERROR: 'red',
};

export function ThreadsSessionPanel() {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [opening, setOpening] = useState(false);
  const [statusError, setStatusError] = useState('');
  const [openLoginError, setOpenLoginError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  async function refreshSessionStatus() {
    setLoading(true);
    setStatusError('');

    try {
      const nextSession = await fetchThreadsSessionStatus();
      setSession(nextSession);
    } catch (nextError) {
      setStatusError(nextError.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleOpenLoginBrowser() {
    setOpening(true);
    setStatusError('');
    setOpenLoginError('');
    setActionMessage('');

    try {
      const response = await openThreadsLoginBrowser();
      setActionMessage(response.message ?? 'Chrome을 열었습니다. 열린 Chrome에서 로그인 후 다시 확인하세요.');

      try {
        const nextSession = await fetchThreadsSessionStatus();
        setSession(nextSession);

        if (nextSession.status === 'LOGIN_REQUIRED') {
          setActionMessage('Chrome을 열었습니다. 열린 Chrome에서 로그인 후 다시 확인하세요.');
        }
      } catch (nextError) {
        setStatusError(nextError.message);
      }
    } catch (nextError) {
      setOpenLoginError(nextError.message);
    } finally {
      setOpening(false);
    }
  }

  useEffect(() => {
    let ignore = false;

    async function loadSessionStatus() {
      setLoading(true);
      setStatusError('');

      try {
        const nextSession = await fetchThreadsSessionStatus();

        if (!ignore) {
          setSession(nextSession);
        }
      } catch (nextError) {
        if (!ignore) {
          setStatusError(nextError.message);
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
  const statusTone = sessionStatusTones[status] ?? sessionStatusTones.ERROR;
  const busy = loading || opening;

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-base font-semibold text-slate-950">Threads 세션</h2>
            <StatusBadge tone={statusTone} testId="threads-session-status">
              {loading && !session ? '확인 중' : statusLabel}
            </StatusBadge>
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
        <StateMessage tone="success" className="mt-3">
          {actionMessage}
        </StateMessage>
      ) : null}

      {openLoginError ? (
        <StateMessage tone="error" className="mt-3">
          로그인 브라우저 열기 요청이 실패했습니다. {openLoginError}
        </StateMessage>
      ) : null}

      {statusError ? (
        <StateMessage tone="error" className="mt-3">
          Threads 세션 상태를 확인하지 못했습니다. {statusError}
        </StateMessage>
      ) : null}
    </section>
  );
}
