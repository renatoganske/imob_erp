package com.imobcrm.financial.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RN-06: job diario (6h de Brasilia) que atualiza para ATRASADO todo lancamento PENDENTE cujo dueDate
 * ja passou. Processa tenant a tenant (log e falha isolados) e usa lock distribuido: com varias
 * instancias, so uma executa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueJob {

    private final OverdueService overdueService;
    private final Clock clock;

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "financial-overdue-job", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void markOverdueEntries() {
        LocalDate today = LocalDate.now(clock);
        overdueService.tenantsWithOverdue(today).forEach(tenantId -> markForTenant(tenantId, today));
    }

    private void markForTenant(UUID tenantId, LocalDate today) {
        try {
            int updated = overdueService.markOverdue(tenantId, today);
            log.info("Job financeiro {}: tenant={} {} lancamento(s) marcado(s) como ATRASADO", today, tenantId, updated);
        } catch (RuntimeException e) {
            log.error("Job financeiro {}: falha ao marcar atrasados do tenant={}", today, tenantId, e);
        }
    }
}
