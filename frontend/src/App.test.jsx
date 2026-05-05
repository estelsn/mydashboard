import '@testing-library/jest-dom/vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  fetchDashboardSummary,
  fetchInfoItems,
} from './api/dashboardApi';
import App from './App';

vi.mock('./api/dashboardApi', () => ({
  archiveInfoItem: vi.fn(),
  fetchDashboardSummary: vi.fn(),
  fetchInfoItems: vi.fn(),
  restoreInfoItem: vi.fn(),
  updateInfoItemDecisionStatus: vi.fn(),
}));

vi.mock('./ThreadsSessionPanel', () => ({
  ThreadsSessionPanel: () => <section>Threads session panel</section>,
}));

vi.mock('./ThreadsCollectionPanel', () => ({
  ThreadsCollectionPanel: () => <section>Threads collection panel</section>,
}));

vi.mock('./SourceManagementPanel', () => ({
  SourceManagementPanel: () => <section>Source management panel</section>,
}));

describe('App dashboard states', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders empty dashboard sections safely', async () => {
    fetchDashboardSummary.mockResolvedValue({
      visibleCount: 0,
      applyCount: 0,
      holdCount: 0,
      unreviewedCount: 0,
      hiddenCount: 0,
    });
    fetchInfoItems.mockResolvedValue([]);

    render(<App />);

    expect(await screen.findByText('오늘 바로 확인할 항목이 없습니다.')).toBeInTheDocument();
    expect(screen.getByText('나중에 볼 항목이 없습니다.')).toBeInTheDocument();
    expect(screen.getByText('검토가 필요한 항목이 없습니다.')).toBeInTheDocument();
  });

  it('renders item metadata, manual override, scores, and evaluation reason', async () => {
    fetchDashboardSummary.mockResolvedValue({
      visibleCount: 1,
      applyCount: 1,
      holdCount: 0,
      unreviewedCount: 0,
      hiddenCount: 0,
    });
    fetchInfoItems.mockResolvedValue([
      {
        id: 1,
        title: 'New model release',
        summary: 'Important update',
        sourceName: 'OpenAI',
        originalUrl: 'https://example.com/post',
        category: 'AI',
        decisionStatus: 'APPLY',
        importanceLevel: 'HIGH',
        manualOverride: true,
        hidden: false,
        publishedAt: '2026-05-05T10:15:00',
        collectedAt: '2026-05-05T10:20:00',
        latestEvaluation: {
          relevanceScore: 0.9,
          actionabilityScore: 0.75,
          noveltyScore: 0.8,
          confidence: 1,
          reason: 'Matches the dashboard focus.',
          createdAt: '2026-05-05T10:21:00',
        },
      },
    ]);

    render(<App />);

    expect(await screen.findByText('New model release')).toBeInTheDocument();
    expect(screen.getByText('수동 반영')).toBeInTheDocument();
    expect(screen.getByText('평가 이유:')).toBeInTheDocument();
    expect(screen.getByText('Matches the dashboard focus.')).toBeInTheDocument();
    expect(screen.getByText('0.90')).toBeInTheDocument();
    expect(screen.getByText(/게시 .* · 수집 /)).toBeInTheDocument();
  });

  it('renders API errors without crashing the dashboard shell', async () => {
    fetchDashboardSummary.mockRejectedValue(new Error('network failed'));
    fetchInfoItems.mockResolvedValue([]);

    render(<App />);

    expect(
      await screen.findByText('백엔드 API를 불러오지 못했습니다. network failed'),
    ).toBeInTheDocument();
    expect(screen.getByText('Threads session panel')).toBeInTheDocument();
  });
});
