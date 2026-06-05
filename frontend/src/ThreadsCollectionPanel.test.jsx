import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { collectRecentThreads, collectThreads, fetchCollectionRuns } from './api/dashboardApi';
import { ThreadsCollectionPanel } from './ThreadsCollectionPanel';

vi.mock('./api/dashboardApi', () => ({
  collectRecentThreads: vi.fn(),
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
          status: 'RUNNING',
          totalSourceCount: 1,
          successfulSourceCount: 0,
          failedSourceCount: 0,
          collectedItemCount: 0,
          createdCount: 0,
          duplicateCount: 0,
          failedCount: 0,
          failureReason: null,
          statusMessage: '진행 중: 전체 1개 소스 중 0개 처리 완료',
        },
      ])
      .mockResolvedValueOnce([
        {
          id: 10,
          status: 'SUCCEEDED',
          totalSourceCount: 1,
          successfulSourceCount: 1,
          failedSourceCount: 0,
          collectedItemCount: 3,
          createdCount: 2,
          duplicateCount: 1,
          failedCount: 0,
          failureReason: null,
          statusMessage: '수집 완료',
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
      requestedMaxScrollCount: 0,
      appliedMaxScrollCount: 0,
      safetyMessage: '안전 제한을 적용해 수집했습니다.',
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={onDashboardRefresh} />);

    expect(await screen.findByText('수집 실행 기록 없음')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(screen.getByRole('button', { name: '수집 실행 중' })).toBeDisabled();
    expect(await screen.findByText('실행 결과: 수집 3, 생성 2, 중복 1, 실패 0')).toBeInTheDocument();
    expect(screen.getByText('적용 제한: 계정당 게시물 3/3')).toBeInTheDocument();
    expect(screen.getByText('안전 제한을 적용해 수집했습니다.')).toBeInTheDocument();
    expect(screen.getByText('실행 #10')).toBeInTheDocument();
    expect(screen.getByText('수집 3, 생성 2, 중복 1, 실패 0')).toBeInTheDocument();
    expect(collectThreads).toHaveBeenCalledWith({
      accountUrls: ['https://www.threads.com/@example'],
      maxPostsPerAccount: 3,
      maxScrollCount: 0,
    });
    expect(fetchCollectionRuns.mock.calls.length).toBeGreaterThanOrEqual(3);
    expect(onDashboardRefresh).toHaveBeenCalledTimes(1);
  });

  it('runs recent three-day collection across enabled sources', async () => {
    const onDashboardRefresh = vi.fn().mockResolvedValue(undefined);
    fetchCollectionRuns
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        {
          id: 20,
          status: 'RUNNING',
          totalSourceCount: 2,
          successfulSourceCount: 1,
          failedSourceCount: 0,
          collectedItemCount: 1,
          createdCount: 0,
          duplicateCount: 0,
          failedCount: 0,
          failureReason: null,
          statusMessage: '진행 중: 전체 2개 소스 중 1개 처리 완료',
        },
      ])
      .mockResolvedValueOnce([
        {
          id: 20,
          status: 'SUCCEEDED',
          totalSourceCount: 2,
          successfulSourceCount: 2,
          failedSourceCount: 0,
          collectedItemCount: 4,
          createdCount: 3,
          duplicateCount: 1,
          failedCount: 0,
          failureReason: null,
          statusMessage: '수집 완료',
        },
      ]);
    collectRecentThreads.mockResolvedValue({
      runId: 20,
      status: 'SUCCEEDED',
      collectedCount: 4,
      createdCount: 3,
      duplicateCount: 1,
      failedCount: 0,
      failureReason: null,
      requestedMaxPostsPerAccount: 3,
      appliedMaxPostsPerAccount: 3,
      requestedMaxScrollCount: 0,
      appliedMaxScrollCount: 0,
      safetyMessage: '5초 간격으로 활성 Threads 소스를 순차 수집했고, 최근 3일 필터를 적용했습니다.',
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={onDashboardRefresh} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.click(screen.getByRole('button', { name: '최근 3일 수집' }));

    expect(screen.getByRole('button', { name: '최근 3일 수집 중' })).toBeDisabled();
    expect(await screen.findByText('실행 결과: 수집 4, 생성 3, 중복 1, 실패 0')).toBeInTheDocument();
    expect(screen.getByText(/최근 3일 필터/)).toBeInTheDocument();
    expect(collectRecentThreads).toHaveBeenCalledTimes(1);
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
      failureReason: 'Threads 로그인이 필요합니다.',
      requestedMaxPostsPerAccount: 3,
      appliedMaxPostsPerAccount: 3,
      requestedMaxScrollCount: 0,
      appliedMaxScrollCount: 0,
      safetyMessage: 'Threads 로그인이 필요해 수집을 중단했습니다.',
    });

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    expect(await screen.findByText('실패 사유: Threads 로그인이 필요합니다.')).toBeInTheDocument();
    expect(screen.getByText('Threads 로그인이 필요해 수집을 중단했습니다.')).toBeInTheDocument();
  });

  it('shows conservative defaults and safety guidance', async () => {
    fetchCollectionRuns.mockResolvedValue([]);

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');

    expect(screen.getByLabelText('계정당 최대 게시물')).toHaveValue(3);
    expect(
      screen.getByText(/현재 구조에서는 게시물 날짜를 기준으로 최신순 정렬 후 상위 결과만 저장합니다/),
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
      failureReason: status === 'COOLDOWN_SKIPPED' ? null : `수집 상태가 ${status} 이어서 중단했습니다.`,
      requestedMaxPostsPerAccount: 3,
      appliedMaxPostsPerAccount: 3,
      requestedMaxScrollCount: 0,
      appliedMaxScrollCount: 0,
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
    collectThreads.mockRejectedValue(new Error('요청 실패: 409'));

    render(<ThreadsCollectionPanel onDashboardRefresh={vi.fn()} />);

    await screen.findByText('수집 실행 기록 없음');
    fireEvent.change(screen.getByLabelText('계정 URL'), {
      target: { value: 'https://www.threads.com/@example' },
    });
    fireEvent.click(screen.getByRole('button', { name: '수집 실행' }));

    await waitFor(() => {
      expect(
        screen.getByText('수집 API를 호출하지 못했습니다. 요청 실패: 409'),
      ).toBeInTheDocument();
    });
  });
});
