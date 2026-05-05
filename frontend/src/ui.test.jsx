import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StateMessage, StatusBadge, formatDateTime, formatScore } from './ui';

describe('shared UI display helpers', () => {
  it('renders status badges and state messages with stable text', () => {
    render(
      <>
        <StatusBadge tone="teal" testId="badge">
          성공
        </StatusBadge>
        <StateMessage tone="error">API 오류</StateMessage>
      </>,
    );

    expect(screen.getByTestId('badge')).toHaveTextContent('성공');
    expect(screen.getByText('API 오류')).toBeInTheDocument();
  });

  it('formats missing and invalid values safely', () => {
    expect(formatDateTime(null)).toBe('-');
    expect(formatDateTime('not-a-date')).toBe('-');
    expect(formatScore(null)).toBe('-');
    expect(formatScore(0.875)).toBe('0.88');
  });
});
