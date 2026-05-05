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
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={onDashboardRefresh} />);

    expect(await screen.findByText('수집 실행 기록 없음')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.change(screen.getByLabelText('계정당 최대 게시물'), {
      target: { value: '12' },
    });
    fireEvent.change(screen.getByLabelText('최대 스크롤'), {
      target: { value: '4' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(screen.getByRole('button', { name: '수집 실행 중' })).toBeDisabled();
    expect(await screen.findByText('실행 결과: 수집 3, 생성 2, 중복 1, 실패 0')).toBeInTheDocument();
    expect(screen.getByText('Run #10')).toBeInTheDocument();
    expect(screen.getByText('수집 3, 생성 2, 중복 1, 실패 0')).toBeInTheDocument();
    expect(collectThreads).toHaveBeenCalledWith({
      accountUrls: ['https://www.threads.com/@example'],
      maxPostsPerAccount: 12,
      maxScrollCount: 4,
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
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(await screen.findByText('실패 사유: Threads login required')).toBeInTheDocument();
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
