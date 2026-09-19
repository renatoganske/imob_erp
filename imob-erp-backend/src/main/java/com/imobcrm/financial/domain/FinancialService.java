package com.imobcrm.financial.domain;

import com.imobcrm.financial.api.FinancialDashboardResponse;
import com.imobcrm.financial.api.FinancialEntryRequest;
import com.imobcrm.financial.api.FinancialEntryResponse;
import com.imobcrm.financial.domain.enums.EntryCategory;
import com.imobcrm.financial.domain.enums.EntryStatus;
import com.imobcrm.financial.domain.enums.EntryType;
import com.imobcrm.financial.infra.FinancialMapper;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialRepository financialRepository;
    private final FinancialMapper financialMapper;

    @Transactional(readOnly = true)
    public Page<FinancialEntryResponse> search(EntryType type, EntryStatus status,
                                                    EntryCategory category, LocalDate from, LocalDate to,
                                                    Pageable pageable) {
        return financialRepository
                .search(TenantContext.tenantId(), type, status, category, from, to, pageable)
                .map(financialMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public FinancialEntryResponse findById(UUID id) {
        return financialMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public FinancialEntryResponse create(FinancialEntryRequest request) {
        FinancialEntry entry = FinancialEntry.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .type(request.type())
                .category(request.category())
                .description(request.description())
                .value(request.value())
                .dueDate(request.dueDate())
                .status(EntryStatus.PENDENTE)
                .recurrent(false)
                .build();
        return financialMapper.toResponseDTO(financialRepository.save(entry));
    }

    @Transactional
    public FinancialEntryResponse pay(UUID id) {
        FinancialEntry entry = findOwned(id);
        // RN-07: lancamento pago nao pode ser editado — apenas cancelado.
        if (entry.getStatus() == EntryStatus.PAGO) {
            throw new BusinessException("Lancamento ja esta pago", "ENTRY_ALREADY_PAID");
        }
        entry.setStatus(EntryStatus.PAGO);
        entry.setPaidAt(LocalDate.now());
        log.info("Lancamento financeiro {} pago: type={} value={}", id, entry.getType(), entry.getValue());
        return financialMapper.toResponseDTO(financialRepository.save(entry));
    }

    @Transactional
    public FinancialEntryResponse cancel(UUID id) {
        FinancialEntry entry = findOwned(id);
        entry.setStatus(EntryStatus.CANCELADO);
        log.info("Lancamento financeiro {} cancelado", id);
        return financialMapper.toResponseDTO(financialRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse dashboard() {
        UUID tenantId = TenantContext.tenantId();
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal recebidoNoMes = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, EntryType.RECEITA, EntryStatus.PAGO, firstDayOfMonth, lastDayOfMonth);
        BigDecimal pagoNoMes = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, EntryType.DESPESA, EntryStatus.PAGO, firstDayOfMonth, lastDayOfMonth);
        BigDecimal aReceber = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, EntryType.RECEITA, EntryStatus.PENDENTE, firstDayOfMonth, lastDayOfMonth);
        BigDecimal aPagar = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, EntryType.DESPESA, EntryStatus.PENDENTE, firstDayOfMonth, lastDayOfMonth);
        BigDecimal inadimplencia = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, EntryType.RECEITA, EntryStatus.ATRASADO, firstDayOfMonth, lastDayOfMonth);

        return new FinancialDashboardResponse(recebidoNoMes.subtract(pagoNoMes), aReceber, aPagar, inadimplencia);
    }

    /**
     * RN-05: gera parcelas de aluguel para os proximos 12 meses ao ativar
     * um contrato de locacao. Chamado pelo ContractService#activate.
     */
    @Transactional
    public void generateRentInstallments(UUID contractId, BigDecimal monthlyValue, LocalDate startDate) {
        for (int i = 0; i < 12; i++) {
            FinancialEntry entry = FinancialEntry.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TenantContext.tenantId())
                    .contractId(contractId)
                    .type(EntryType.RECEITA)
                    .category(EntryCategory.ALUGUEL)
                    .description("Aluguel - parcela " + (i + 1) + "/12")
                    .value(monthlyValue)
                    .dueDate(startDate.plusMonths(i))
                    .status(EntryStatus.PENDENTE)
                    .recurrent(true)
                    .build();
            financialRepository.save(entry);
        }
        log.info("12 parcelas de aluguel geradas para o contrato {}: valor mensal={}", contractId, monthlyValue);
    }

    @Transactional
    public void createSingleEntry(UUID contractId, EntryType type, EntryCategory category,
                                   String description, BigDecimal value, LocalDate dueDate) {
        FinancialEntry entry = FinancialEntry.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .contractId(contractId)
                .type(type)
                .category(category)
                .description(description)
                .value(value)
                .dueDate(dueDate)
                .status(EntryStatus.PENDENTE)
                .recurrent(false)
                .build();
        financialRepository.save(entry);
    }

    private FinancialEntry findOwned(UUID id) {
        return financialRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Lancamento financeiro", id));
    }
}
