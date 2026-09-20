package com.imobcrm.financial;

import com.imobcrm.financial.domain.OverdueJob;
import com.imobcrm.financial.domain.OverdueService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unitario do job de atrasados (IMOB-27): agenda, data de Brasilia, isolamento de falha por tenant. */
class OverdueJobTest {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final OverdueService service = mock(OverdueService.class);

    private OverdueJob jobAt(String instant) {
        return new OverdueJob(service, Clock.fixed(Instant.parse(instant), BRASILIA));
    }

    @Test
    void isScheduledAtSixInBrasiliaAndLocked() throws Exception {
        var method = OverdueJob.class.getMethod("markOverdueEntries");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertEquals("0 0 6 * * *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
        assertNotNull(method.getAnnotation(SchedulerLock.class));
    }

    @Test
    void usesBrasiliaDateNotUtc() {
        // 02:00Z de 10/05 ainda e 23:00 de 09/05 em Brasilia
        when(service.tenantsWithOverdue(any())).thenReturn(List.of());

        jobAt("2026-05-10T02:00:00Z").markOverdueEntries();

        verify(service).tenantsWithOverdue(LocalDate.of(2026, 5, 9));
    }

    @Test
    void processesEachTenantAndKeepsGoingWhenOneFails() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 5, 10);
        when(service.tenantsWithOverdue(today)).thenReturn(List.of(a, b));
        when(service.markOverdue(a, today)).thenThrow(new IllegalStateException("boom"));
        when(service.markOverdue(b, today)).thenReturn(3);

        jobAt("2026-05-10T09:00:00Z").markOverdueEntries();

        verify(service).markOverdue(a, today);
        verify(service).markOverdue(b, today);
    }

    @Test
    void doesNothingWhenNoTenantHasOverdue() {
        when(service.tenantsWithOverdue(any())).thenReturn(List.of());

        jobAt("2026-05-10T09:00:00Z").markOverdueEntries();

        verify(service, never()).markOverdue(any(), any());
    }
}
