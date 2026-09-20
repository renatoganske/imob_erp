package com.imobcrm.financial.domain;

import com.imobcrm.financial.domain.enums.EntryCategory;
import com.imobcrm.financial.domain.enums.EntryStatus;
import com.imobcrm.financial.domain.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialRepository {

    <S extends FinancialEntry> S save(S entity);

    <S extends FinancialEntry> List<S> saveAll(Iterable<S> entities);

    Optional<FinancialEntry> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<FinancialEntry> search(
            UUID tenantId,
            EntryType type,
            EntryStatus status,
            EntryCategory category,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    List<FinancialEntry> findAllByTenantIdAndStatusAndDueDateBefore(UUID tenantId, EntryStatus status, LocalDate date);

    List<FinancialEntry> findAllByStatusAndDueDateBefore(EntryStatus status, LocalDate date);

    /** Soma dos lancamentos com vencimento ate a data (inclusive), sem limite inferior. */
    java.math.BigDecimal sumByTypeAndStatusDueUntil(UUID tenantId, EntryType type, EntryStatus status, LocalDate to);

    java.math.BigDecimal sumByTypeAndStatusInPeriod(
            UUID tenantId,
            EntryType type,
            EntryStatus status,
            LocalDate from,
            LocalDate to);
}
