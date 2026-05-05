import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchThreadsSessionStatus, openThreadsLoginBrowser } from './api/dashboardApi';
import { ThreadsSessionPanel } from './ThreadsSessionPanel';

vi.mock('./api/dashboardApi', () => ({
  fetchThreadsSessionStatus: vi.fn(),
  openThreadsLoginBrowser: vi.fn(),
}));

describe('ThreadsSessionPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it.each([
    ['NOT_CONFIGURED', '설정 필요'],
    ['LOGIN_REQUIRED', '로그인 필요'],
    ['READY', '준비됨'],
    ['EXPIRED', '만료됨'],
    ['ERROR', '오류'],
  ])('renders %s session status', async (status, label) => {
    fetchThreadsSessionStatus.mockResolvedValue({
      status,
      profilePath: '/tmp/threads-profile',
      message: `${status} message`,
    });

    render(<ThreadsSessionPanel />);

    expect(await screen.findByTestId('threads-session-status')).toHaveTextContent(label);
    expect(screen.getByText(`${status} message`)).toBeInTheDocument();
    expect(screen.getByText('프로필: /tmp/threads-profile')).toBeInTheDocument();
  });

  it('refreshes session status with a loading state', async () => {
    fetchThreadsSessionStatus
      .mockResolvedValueOnce({
        status: 'LOGIN_REQUIRED',
        profilePath: null,
        message: 'login needed',
      })
      .mockResolvedValueOnce({
        status: 'READY',
        profilePath: '/tmp/ready-profile',
        message: 'ready',
      });

    render(<ThreadsSessionPanel />);

    expect(await screen.findByText('login needed')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '세션 상태 다시 확인' }));

    expect(screen.getByRole('button', { name: '확인 중' })).toBeDisabled();
    expect(await screen.findByText('ready')).toBeInTheDocument();
    expect(screen.getByTestId('threads-session-status')).toHaveTextContent('준비됨');
    expect(fetchThreadsSessionStatus).toHaveBeenCalledTimes(2);
  });

  it('opens the login browser and reloads session status', async () => {
    fetchThreadsSessionStatus
      .mockResolvedValueOnce({
        status: 'LOGIN_REQUIRED',
        profilePath: null,
        message: 'login needed',
      })
      .mockResolvedValueOnce({
        status: 'READY',
        profilePath: '/tmp/ready-profile',
        message: 'ready',
      });
    openThreadsLoginBrowser.mockResolvedValue({
      status: 'OPENED',
      profilePath: '/tmp/threads-profile',
      loginUrl: 'https://www.threads.net/',
      message: 'browser opened',
    });

    render(<ThreadsSessionPanel />);

    await screen.findByText('login needed');
    fireEvent.click(screen.getByRole('button', { name: '로그인 브라우저 열기' }));

    expect(screen.getByRole('button', { name: '여는 중' })).toBeDisabled();
    expect(await screen.findByText('browser opened')).toBeInTheDocument();
    expect(screen.getByTestId('threads-session-status')).toHaveTextContent('준비됨');
    expect(openThreadsLoginBrowser).toHaveBeenCalledTimes(1);
    expect(fetchThreadsSessionStatus).toHaveBeenCalledTimes(2);
  });

  it('shows an error message when the status API fails', async () => {
    fetchThreadsSessionStatus.mockRejectedValue(new Error('network failed'));

    render(<ThreadsSessionPanel />);

    expect(
      await screen.findByText('Threads 세션 API를 호출하지 못했습니다. network failed'),
    ).toBeInTheDocument();
  });

  it('shows an error message when opening the login browser fails', async () => {
    fetchThreadsSessionStatus.mockResolvedValue({
      status: 'LOGIN_REQUIRED',
      profilePath: null,
      message: 'login needed',
    });
    openThreadsLoginBrowser.mockRejectedValue(new Error('chrome missing'));

    render(<ThreadsSessionPanel />);

    await screen.findByText('login needed');
    fireEvent.click(screen.getByRole('button', { name: '로그인 브라우저 열기' }));

    await waitFor(() => {
      expect(
        screen.getByText('Threads 세션 API를 호출하지 못했습니다. chrome missing'),
      ).toBeInTheDocument();
    });
  });
});
