import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { collectThreads, fetchCollectionRuns } from './api/dashboardApi';
import { ThreadsCollectionPanel } from './ThreadsCollectionPanel';

vi.mock('./api/dashboardApi', () => ({
  collectThreads: vi.fn(),
  fetchCollectionRuns: vi.fn(),
}));

describe('ThreadsCollectionPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('runs manual Threads collection and refreshes collection runs and dashboard', async () => {
    const onDashboardRefresh = vi.fn().mockResolvedValue(undefined);
    fetchCollectionRuns
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        {
          id: 10,
          status: 'SUCCEEDED',
          collectedItemCount: 3,
          createdCount: 2,
          duplicateCount: 1,
          failedCount: 0,
          failureReason: null,
        },
      ]);
    collectThreads.mockResolvedValue({
      runId: 10,
      status: 'SUCCEEDED',
      collectedCount: 3,
      createdCount: 2,
      duplicateCount: 1,
      failedCount: 0,
      failureReason: null,
      requestedMaxPostsPerAccount: 3,
      appliedMaxPostsPerAccount: 3,
      requestedMaxScrollCount: 1,
      appliedMaxScrollCount: 1,
      safetyMessage: 'Threads collection safety limits applied.',
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={onDashboardRefresh} />);

    expect(await screen.findByText('수집 실행 기록 없음')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(screen.getByRole('button', { name: '수집 실행 중' })).toBeDisabled();
    expect(await screen.findByText('실행 결과: 수집 3, 생성 2, 중복 1, 실패 0')).toBeInTheDocument();
    expect(screen.getByText('적용 제한: 계정당 게시물 3/3, 스크롤 1/1')).toBeInTheDocument();
    expect(screen.getByText('Threads collection safety limits applied.')).toBeInTheDocument();
    expect(screen.getByText('Run #10')).toBeInTheDocument();
    expect(screen.getByText('수집 3, 생성 2, 중복 1, 실패 0')).toBeInTheDocument();
    expect(collectThreads).toHaveBeenCalledWith({
      accountUrls: ['https://www.threads.com/@example'],
      maxPostsPerAccount: 3,
      maxScrollCount: 1,
    });
    expect(fetchCollectionRuns).toHaveBeenCalledTimes(2);
    expect(onDashboardRefresh).toHaveBeenCalledTimes(1);
  });

  it('shows failureReason returned by manual collection', async () => {
    fetchCollectionRuns.mockResolvedValue([]);
    collectThreads.mockResolvedValue({
      runId: 11,
      status: 'FAILED',
      collectedCount: 0,
      createdCount: 0,
      duplicateCount: 0,
      failedCount: 0,
      failureReason: 'Threads login required',
      requestedMaxPostsPerAccount: 3,
      appliedMaxPostsPerAccount: 3,
      requestedMaxScrollCount: 1,
      appliedMaxScrollCount: 1,
      safetyMessage: 'Stopped because Threads session status was LOGIN_REQUIRED.',
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(await screen.findByText('실패 사유: Threads login required')).toBeInTheDocument();
    expect(screen.getByText('세션 확인 필요: Threads 로그인이 필요합니다.')).toBeInTheDocument();
  });

  it('shows conservative defaults and safety guidance', async () => {
    fetchCollectionRuns.mockResolvedValue([]);

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');

    expect(screen.getByLabelText('계정당 최대 게시물')).toHaveValue(3);
    expect(screen.getByLabelText('최대 스크롤')).toHaveValue(1);
    expect(
      screen.getByText(/Threads 수집은 계정 보호를 위해 낮은 기본값과 대기 시간을 적용합니다/),
    ).toBeInTheDocument();
  });

  it.each([
    ['ACCESS_RESTRICTED', '접근 제한 가능성: 수집을 즉시 중단했습니다.'],
    ['TIMEOUT', '시간 초과: 수집을 즉시 중단했습니다.'],
    ['COOLDOWN_SKIPPED', '최근 수집된 Source라서 쿨다운 정책에 따라 건너뛰었습니다.'],
  ])('shows distinct safety message for %s', async (status, message) => {
    fetchCollectionRuns.mockResolvedValue([]);
    collectThreads.mockResolvedValue({
      runId: 12,
      status: status === 'COOLDOWN_SKIPPED' ? 'SUCCEEDED' : 'FAILED',
      collectedCount: 0,
      createdCount: 0,
      duplicateCount: 0,
      failedCount: status === 'COOLDOWN_SKIPPED' ? 0 : 1,
      failureReason: status === 'COOLDOWN_SKIPPED' ? null : `Threads collection failed: status=${status}`,
      requestedMaxPostsPerAccount: 3,
      appliedMaxPostsPerAccount: 3,
      requestedMaxScrollCount: 1,
      appliedMaxScrollCount: 1,
      safetyMessage: status,
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(await screen.findByText(message)).toBeInTheDocument();
  });

  it('shows API errors when manual collection request fails', async () => {
    fetchCollectionRuns.mockResolvedValue([]);
    collectThreads.mockRejectedValue(new Error('API request failed: 409'));

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    await waitFor(() => {
      expect(
        screen.getByText('Threads 수집 API를 호출하지 못했습니다. API request failed: 409'),
      ).toBeInTheDocument();
    });
  });
});
