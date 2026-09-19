package com.imobcrm.financial.domain;

import com.imobcrm.financial.domain.enums.EntryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * RN-06: job diario que atualiza para ATRASADO todo lancamento PENDENTE
 * cujo dueDate ja passou.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueJob {

    private final FinancialRepository financialRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void markOverdueEntries() {
        List<FinancialEntry> overdue = financialRepository
                .findAllByStatusAndDueDateBefore(EntryStatus.PENDENTE, LocalDate.now());
        overdue.forEach(entry -> entry.setStatus(EntryStatus.ATRASADO));
        financialRepository.saveAll(overdue);
        log.info("Job financeiro: {} lancamento(s) marcado(s) como ATRASADO", overdue.size());
    }
}
