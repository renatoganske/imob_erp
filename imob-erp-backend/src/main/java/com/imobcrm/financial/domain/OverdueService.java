package com.imobcrm.financial.domain;

import com.imobcrm.financial.domain.enums.EntryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** RN-06: marca como ATRASADO os lancamentos PENDENTE vencidos, uma transacao por tenant. */
@Service
@RequiredArgsConstructor
public class OverdueService {

    private final FinancialRepository financialRepository;

    public List<UUID> tenantsWithOverdue(LocalDate today) {
        return financialRepository.findTenantIdsByStatusAndDueDateBefore(EntryStatus.PENDENTE, today);
    }

    /** Idempotente: depois da primeira execucao nao sobra PENDENTE vencido, entao a repeticao devolve 0. */
    @Transactional
    public int markOverdue(UUID tenantId, LocalDate today) {
        return financialRepository.updateStatusByTenantAndDueDateBefore(
                tenantId, EntryStatus.PENDENTE, EntryStatus.ATRASADO, today);
    }
}
