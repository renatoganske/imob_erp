package com.imobcrm.financial;

import com.imobcrm.financial.domain.OverdueService;
import com.imobcrm.support.IntegrationTestBase;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Job de atrasados (IMOB-27) contra PostgreSQL real: escopo por tenant, regra de data, idempotencia e lock. */
class OverdueServiceTest extends IntegrationTestBase {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 15);
    private static UUID tenantA;
    private static UUID tenantB;

    @Autowired
    private OverdueService service;
    @Autowired
    private LockProvider lockProvider;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Over A', ?, 'STARTER')", tenantA, "over-a-" + RUN);
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Over B', ?, 'STARTER')", tenantB, "over-b-" + RUN);
        entry(jdbc, tenantA, "PENDENTE", "2031-03-14");   // vencido -> ATRASADO
        entry(jdbc, tenantA, "PENDENTE", "2031-01-01");   // vencido -> ATRASADO
        entry(jdbc, tenantA, "PENDENTE", "2031-03-15");   // vence hoje -> continua PENDENTE
        entry(jdbc, tenantA, "PENDENTE", "2031-03-16");   // futuro -> continua PENDENTE
        entry(jdbc, tenantA, "PAGO", "2031-01-10");       // pago -> intacto
        entry(jdbc, tenantB, "PENDENTE", "2031-02-01");   // outro tenant
    }

    private static void entry(JdbcTemplate jdbc, UUID tenant, String status, String dueDate) {
        jdbc.update("INSERT INTO financial_entries (id, tenant_id, type, category, description, value, due_date, status) "
                + "VALUES (?, ?, 'RECEITA', 'OUTRO', 'teste', 100, ?::date, ?)", UUID.randomUUID(), tenant, dueDate, status);
    }

    private int count(UUID tenant, String status) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM financial_entries WHERE tenant_id = ? AND status = ?", Integer.class, tenant, status);
    }

    @Test
    void marksOnlyPastDuePendingOfTheGivenTenantAndIsIdempotent() {
        assertTrue(service.tenantsWithOverdue(TODAY).containsAll(java.util.List.of(tenantA, tenantB)));

        assertEquals(2, service.markOverdue(tenantA, TODAY));

        assertEquals(2, count(tenantA, "ATRASADO"));
        assertEquals(2, count(tenantA, "PENDENTE"));
        assertEquals(1, count(tenantA, "PAGO"));
        assertEquals(1, count(tenantB, "PENDENTE"), "outro tenant nao pode ser tocado");

        assertEquals(0, service.markOverdue(tenantA, TODAY), "segunda execucao no mesmo dia nao muda nada");
        assertEquals(2, count(tenantA, "ATRASADO"));
    }

    @Test
    void lockAllowsOnlyOneHolderAtATime() {
        LockConfiguration config = new LockConfiguration(
                Instant.now(), "test-lock-" + RUN, Duration.ofMinutes(1), Duration.ZERO);

        Optional<SimpleLock> first = lockProvider.lock(config);
        Optional<SimpleLock> second = lockProvider.lock(config);

        assertTrue(first.isPresent());
        assertTrue(second.isEmpty(), "com o lock retido, outra instancia nao executa");
        first.get().unlock();
        assertTrue(lockProvider.lock(config).isPresent());
    }
}
