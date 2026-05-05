const badgeToneClassNames = {
  slate: 'border-slate-200 bg-slate-100 text-slate-700',
  teal: 'border-teal-200 bg-teal-50 text-teal-800',
  amber: 'border-amber-200 bg-amber-50 text-amber-800',
  orange: 'border-orange-200 bg-orange-50 text-orange-800',
  red: 'border-red-200 bg-red-50 text-red-800',
  sky: 'border-sky-200 bg-sky-50 text-sky-800',
  indigo: 'border-indigo-200 bg-indigo-50 text-indigo-800',
};

const stateToneClassNames = {
  loading: 'border-slate-200 bg-slate-50 text-slate-600',
  empty: 'border-slate-200 bg-slate-50 text-slate-500',
  error: 'border-red-200 bg-red-50 text-red-800',
  success: 'border-teal-200 bg-teal-50 text-teal-900',
  warning: 'border-amber-200 bg-amber-50 text-amber-900',
};

export function StatusBadge({ children, tone = 'slate', testId }) {
  const toneClassName = badgeToneClassNames[tone] ?? badgeToneClassNames.slate;

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${toneClassName}`}
      data-testid={testId}
    >
      {children}
    </span>
  );
}

export function StateMessage({ children, tone = 'empty', className = '' }) {
  const toneClassName = stateToneClassNames[tone] ?? stateToneClassNames.empty;

  return (
    <p className={`rounded-md border px-3 py-3 text-sm ${toneClassName} ${className}`.trim()}>
      {children}
    </p>
  );
}

export function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date);
}

export function formatScore(value) {
  return Number.isFinite(value) ? value.toFixed(2) : '-';
}
