import { describe, expect, it } from 'vitest';
import { groupItemsByDashboardSection } from './dashboardSections';

describe('groupItemsByDashboardSection', () => {
  const items = [
    { id: 1, decisionStatus: 'APPLY' },
    { id: 2, decisionStatus: 'HOLD' },
    { id: 3, decisionStatus: 'UNREVIEWED' },
    { id: 4, decisionStatus: 'IGNORE' },
    { id: 5, decisionStatus: 'ARCHIVE_CANDIDATE' },
  ];

  it('groups primary dashboard sections', () => {
    const groups = groupItemsByDashboardSection(items, false);

    expect(groups.apply.map((item) => item.id)).toEqual([1]);
    expect(groups.hold.map((item) => item.id)).toEqual([2]);
    expect(groups.unreviewed.map((item) => item.id)).toEqual([3]);
    expect(groups.hidden).toEqual([]);
  });

  it('includes ignored and archive candidate items only when requested', () => {
    const groups = groupItemsByDashboardSection(items, true);

    expect(groups.hidden.map((item) => item.id)).toEqual([4, 5]);
  });
});
