import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchSources, updateSourceEnabled } from './api/dashboardApi';
import { SourceManagementPanel } from './SourceManagementPanel';

vi.mock('./api/dashboardApi', () => ({
  fetchSources: vi.fn(),
  updateSourceEnabled: vi.fn(),
}));

describe('SourceManagementPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders source list with enabled state and collection metadata', async () => {
    fetchSources.mockResolvedValue([
      {
        id: 1,
        name: 'OpenAI Threads',
        sourceType: 'THREADS_ACCOUNT',
        url: 'https://www.threads.com/@openai',
        enabled: true,
        priority: 10,
        lastCollectedAt: '2026-05-04T12:30:00',
      },
      {
        id: 2,
        name: 'AI RSS',
        sourceType: 'RSS_FEED',
        url: 'https://example.com/rss.xml',
        enabled: false,
        priority: 80,
        lastCollectedAt: null,
      },
    ]);

    render(<SourceManagementPanel />);

    expect(await screen.findByText('OpenAI Threads')).toBeInTheDocument();
    expect(screen.getByText('AI RSS')).toBeInTheDocument();
    expect(screen.getByText('Threads')).toBeInTheDocument();
    expect(screen.getByText('RSS')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'https://www.threads.com/@openai' })).toHaveAttribute(
      'href',
      'https://www.threads.com/@openai',
    );
    expect(screen.getByLabelText('OpenAI Threads enabled')).toBeChecked();
    expect(screen.getByLabelText('AI RSS enabled')).not.toBeChecked();
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getAllByText('-')).toHaveLength(1);
  });

  it('updates enabled state through the Source API', async () => {
    fetchSources.mockResolvedValue([
      {
        id: 1,
        name: 'OpenAI Threads',
        sourceType: 'THREADS_ACCOUNT',
        url: 'https://www.threads.com/@openai',
        enabled: true,
        priority: 10,
        lastCollectedAt: null,
      },
    ]);
    updateSourceEnabled.mockResolvedValue({
      id: 1,
      name: 'OpenAI Threads',
      sourceType: 'THREADS_ACCOUNT',
      url: 'https://www.threads.com/@openai',
      enabled: false,
      priority: 10,
      lastCollectedAt: null,
    });

    render(<SourceManagementPanel />);

    const checkbox = await screen.findByLabelText('OpenAI Threads enabled');
    fireEvent.click(checkbox);

    expect(screen.getByText('저장 중')).toBeInTheDocument();
    await waitFor(() => {
      expect(updateSourceEnabled).toHaveBeenCalledWith(1, false);
    });
    expect(await screen.findByText('비활성')).toBeInTheDocument();
    expect(screen.getByLabelText('OpenAI Threads enabled')).not.toBeChecked();
  });

  it('shows API errors when source loading fails', async () => {
    fetchSources.mockRejectedValue(new Error('network failed'));

    render(<SourceManagementPanel />);

    expect(await screen.findByText('Source API를 호출하지 못했습니다. network failed')).toBeInTheDocument();
  });
});
