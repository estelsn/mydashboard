export const PRIMARY_SECTIONS = [
  {
    key: 'apply',
    title: '오늘 볼 것',
    decisionStatus: 'APPLY',
    emptyText: '오늘 바로 확인할 항목이 없습니다.',
  },
  {
    key: 'hold',
    title: '나중에 볼 것',
    decisionStatus: 'HOLD',
    emptyText: '나중에 볼 항목이 없습니다.',
  },
  {
    key: 'unreviewed',
    title: '검토 필요',
    decisionStatus: 'UNREVIEWED',
    emptyText: '검토가 필요한 항목이 없습니다.',
  },
];

export const HIDDEN_SECTION = {
  key: 'hidden',
  title: '숨김 항목',
  decisionStatuses: ['IGNORE', 'ARCHIVE_CANDIDATE'],
  emptyText: '숨김 항목이 없습니다.',
};

export function isHiddenDashboardItem(item) {
  return item.hidden || HIDDEN_SECTION.decisionStatuses.includes(item.decisionStatus);
}

export function groupItemsByDashboardSection(items, includeHidden) {
  const groups = Object.fromEntries(PRIMARY_SECTIONS.map((section) => [section.key, []]));
  groups[HIDDEN_SECTION.key] = [];

  for (const item of items) {
    const primarySection = PRIMARY_SECTIONS.find(
      (section) => section.decisionStatus === item.decisionStatus,
    );

    if (primarySection) {
      groups[primarySection.key].push(item);
      continue;
    }

    if (includeHidden && HIDDEN_SECTION.decisionStatuses.includes(item.decisionStatus)) {
      groups[HIDDEN_SECTION.key].push(item);
    }
  }

  return groups;
}

export function applyInfoItemUpdate(items, updatedItem, includeHidden) {
  const shouldKeepItem = includeHidden || !isHiddenDashboardItem(updatedItem);
  const hasExistingItem = items.some((item) => item.id === updatedItem.id);

  if (!hasExistingItem && shouldKeepItem) {
    return [updatedItem, ...items];
  }

  if (!shouldKeepItem) {
    return items.filter((item) => item.id !== updatedItem.id);
  }

  return items.map((item) => (item.id === updatedItem.id ? updatedItem : item));
}
