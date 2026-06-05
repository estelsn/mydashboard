package com.aifomo.dashboard.collector.threads;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThreadsPostDateResolver {

    private static final Pattern ENGLISH_RELATIVE = Pattern.compile(
            "^(\\d+)\\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|wk|wks|week|weeks)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern KOREAN_RELATIVE = Pattern.compile("^(\\d+)\\s*(초|분|시간|일|주)$");
    private static final Pattern KOREAN_MONTH_DAY = Pattern.compile("^(\\d{1,2})월\\s*(\\d{1,2})일$");
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd HH:mm").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd HH:mm").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy.MM.dd HH:mm").toFormatter(Locale.ENGLISH)
    );
    private static final List<DateTimeFormatter> ABSOLUTE_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy.MM.dd").toFormatter(Locale.ENGLISH)
    );
    private static final List<DateTimeFormatter> MONTH_DAY_FORMATTERS = List.of(
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d").toFormatter(Locale.ENGLISH)
    );

    public Optional<LocalDateTime> resolve(String displayTime, Clock clock) {
        if (displayTime == null || displayTime.isBlank()) {
            return Optional.empty();
        }

        String trimmed = displayTime.trim();
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<LocalDateTime> exactTimestamp = parseExactTimestamp(trimmed, clock.getZone());
        if (exactTimestamp.isPresent()) {
            return exactTimestamp;
        }

        Optional<LocalDateTime> relativeTimestamp = parseRelative(trimmed, now);
        if (relativeTimestamp.isPresent()) {
            return relativeTimestamp;
        }

        Optional<LocalDateTime> calendarDate = parseCalendarDate(trimmed, now.toLocalDate());
        if (calendarDate.isPresent()) {
            return calendarDate;
        }

        return Optional.empty();
    }

    private Optional<LocalDateTime> parseExactTimestamp(String value, ZoneId zoneId) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return Optional.of(OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(ZonedDateTime.parse(value).withZoneSameInstant(zoneId).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(LocalDateTime.ofInstant(Instant.parse(value), zoneId));
        } catch (DateTimeParseException ignored) {
        }

        return Optional.empty();
    }

    private Optional<LocalDateTime> parseRelative(String value, LocalDateTime now) {
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.equals("yesterday") || normalized.equals("어제")) {
            return Optional.of(now.minusDays(1));
        }

        Matcher englishRelative = ENGLISH_RELATIVE.matcher(normalized);
        if (englishRelative.matches()) {
            long amount = Long.parseLong(englishRelative.group(1));
            return Optional.of(now.minus(amount, englishUnit(englishRelative.group(2))));
        }

        Matcher koreanRelative = KOREAN_RELATIVE.matcher(value.trim());
        if (koreanRelative.matches()) {
            long amount = Long.parseLong(koreanRelative.group(1));
            return Optional.of(now.minus(amount, koreanUnit(koreanRelative.group(2))));
        }

        return Optional.empty();
    }

    private Optional<LocalDateTime> parseCalendarDate(String value, LocalDate today) {
        Matcher koreanMonthDay = KOREAN_MONTH_DAY.matcher(value);
        if (koreanMonthDay.matches()) {
            int month = Integer.parseInt(koreanMonthDay.group(1));
            int day = Integer.parseInt(koreanMonthDay.group(2));
            return Optional.of(adjustYear(today, month, day).atStartOfDay());
        }

        for (DateTimeFormatter formatter : ABSOLUTE_DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(value, formatter).atStartOfDay());
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : MONTH_DAY_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(
                        value,
                        new DateTimeFormatterBuilder()
                                .parseCaseInsensitive()
                                .append(formatter)
                                .parseDefaulting(java.time.temporal.ChronoField.YEAR, today.getYear())
                                .toFormatter(Locale.ENGLISH)
                );
                return Optional.of(adjustYear(today, parsed.getMonthValue(), parsed.getDayOfMonth()).atStartOfDay());
            } catch (DateTimeParseException ignored) {
            }
        }

        return Optional.empty();
    }

    private LocalDate adjustYear(LocalDate today, int month, int dayOfMonth) {
        LocalDate candidate = LocalDate.of(today.getYear(), month, dayOfMonth);
        if (candidate.isAfter(today.plusDays(1))) {
            return candidate.minusYears(1);
        }
        return candidate;
    }

    private ChronoUnit englishUnit(String unit) {
        return switch (unit.toLowerCase(Locale.ENGLISH)) {
            case "s", "sec", "secs", "second", "seconds" -> ChronoUnit.SECONDS;
            case "m", "min", "mins", "minute", "minutes" -> ChronoUnit.MINUTES;
            case "h", "hr", "hrs", "hour", "hours" -> ChronoUnit.HOURS;
            case "d", "day", "days" -> ChronoUnit.DAYS;
            case "w", "wk", "wks", "week", "weeks" -> ChronoUnit.WEEKS;
            default -> throw new IllegalArgumentException("Unsupported relative unit: " + unit);
        };
    }

    private ChronoUnit koreanUnit(String unit) {
        return switch (unit) {
            case "초" -> ChronoUnit.SECONDS;
            case "분" -> ChronoUnit.MINUTES;
            case "시간" -> ChronoUnit.HOURS;
            case "일" -> ChronoUnit.DAYS;
            case "주" -> ChronoUnit.WEEKS;
            default -> throw new IllegalArgumentException("Unsupported relative unit: " + unit);
        };
    }
}
