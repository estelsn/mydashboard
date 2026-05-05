import { describe, expect, it } from 'vitest';
import { applyInfoItemUpdate, groupItemsByDashboardSection } from './dashboardSections';

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

describe('applyInfoItemUpdate', () => {
  it('replaces an updated visible item', () => {
    const items = [
      { id: 1, title: 'old', decisionStatus: 'UNREVIEWED', hidden: false },
      { id: 2, title: 'other', decisionStatus: 'HOLD', hidden: false },
    ];

    const updated = { id: 1, title: 'new', decisionStatus: 'APPLY', hidden: false };

    expect(applyInfoItemUpdate(items, updated, false)).toEqual([updated, items[1]]);
  });

  it('removes a hidden update when hidden items are excluded', () => {
    const items = [{ id: 1, decisionStatus: 'UNREVIEWED', hidden: false }];
    const updated = { id: 1, decisionStatus: 'ARCHIVE_CANDIDATE', hidden: true };

    expect(applyInfoItemUpdate(items, updated, false)).toEqual([]);
  });

  it('keeps a hidden update when hidden items are included', () => {
    const items = [{ id: 1, decisionStatus: 'UNREVIEWED', hidden: false }];
    const updated = { id: 1, decisionStatus: 'ARCHIVE_CANDIDATE', hidden: true };

    expect(applyInfoItemUpdate(items, updated, true)).toEqual([updated]);
  });
});
