package com.aifomo.dashboard.collector.threads;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadsPostDateResolverTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final ThreadsPostDateResolver resolver = new ThreadsPostDateResolver();

    @Test
    void resolvesIsoAndRelativeDates() {
        assertThat(resolver.resolve("2026-05-05T10:15:00", CLOCK))
                .contains(LocalDateTime.of(2026, 5, 5, 10, 15));
        assertThat(resolver.resolve("3h", CLOCK))
                .contains(LocalDateTime.of(2026, 6, 5, 9, 0));
        assertThat(resolver.resolve("어제", CLOCK))
                .contains(LocalDateTime.of(2026, 6, 4, 12, 0));
    }

    @Test
    void resolvesCalendarDatesWithoutYear() {
        assertThat(resolver.resolve("May 5", CLOCK))
                .contains(LocalDateTime.of(2026, 5, 5, 0, 0));
        assertThat(resolver.resolve("5월 5일", CLOCK))
                .contains(LocalDateTime.of(2026, 5, 5, 0, 0));
    }

    @Test
    void returnsEmptyWhenDisplayTimeIsUnknown() {
        assertThat(resolver.resolve("sometime later", CLOCK)).isEmpty();
    }
}
