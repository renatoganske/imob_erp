package com.imobcrm.financial;

import com.imobcrm.shared.exception.BusinessRuleException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.financial.dto.FinancialDashboardDTO;
import com.imobcrm.financial.dto.FinancialEntryRequestDTO;
import com.imobcrm.financial.dto.FinancialEntryResponseDTO;
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
    public Page<FinancialEntryResponseDTO> search(FinancialType type, FinancialStatus status,
                                                    FinancialCategory category, LocalDate from, LocalDate to,
                                                    Pageable pageable) {
        return financialRepository
                .search(TenantContext.tenantId(), type, status, category, from, to, pageable)
                .map(financialMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public FinancialEntryResponseDTO findById(UUID id) {
        return financialMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public FinancialEntryResponseDTO create(FinancialEntryRequestDTO request) {
        FinancialEntry entry = FinancialEntry.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .type(request.type())
                .category(request.category())
                .description(request.description())
                .value(request.value())
                .dueDate(request.dueDate())
                .status(FinancialStatus.PENDENTE)
                .recurrent(false)
                .build();
        return financialMapper.toResponseDTO(financialRepository.save(entry));
    }

    @Transactional
    public FinancialEntryResponseDTO pay(UUID id) {
        FinancialEntry entry = findOwned(id);
        // RN-07: lancamento pago nao pode ser editado — apenas cancelado.
        if (entry.getStatus() == FinancialStatus.PAGO) {
            throw new BusinessRuleException("Lancamento ja esta pago", "ENTRY_ALREADY_PAID");
        }
        entry.setStatus(FinancialStatus.PAGO);
        entry.setPaidAt(LocalDate.now());
        log.info("Lancamento financeiro {} pago: type={} value={}", id, entry.getType(), entry.getValue());
        return financialMapper.toResponseDTO(financialRepository.save(entry));
    }

    @Transactional
    public FinancialEntryResponseDTO cancel(UUID id) {
        FinancialEntry entry = findOwned(id);
        entry.setStatus(FinancialStatus.CANCELADO);
        log.info("Lancamento financeiro {} cancelado", id);
        return financialMapper.toResponseDTO(financialRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public FinancialDashboardDTO dashboard() {
        UUID tenantId = TenantContext.tenantId();
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal recebidoNoMes = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, FinancialType.RECEITA, FinancialStatus.PAGO, firstDayOfMonth, lastDayOfMonth);
        BigDecimal pagoNoMes = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, FinancialType.DESPESA, FinancialStatus.PAGO, firstDayOfMonth, lastDayOfMonth);
        BigDecimal aReceber = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, FinancialType.RECEITA, FinancialStatus.PENDENTE, firstDayOfMonth, lastDayOfMonth);
        BigDecimal aPagar = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, FinancialType.DESPESA, FinancialStatus.PENDENTE, firstDayOfMonth, lastDayOfMonth);
        BigDecimal inadimplencia = financialRepository.sumByTypeAndStatusInPeriod(
                tenantId, FinancialType.RECEITA, FinancialStatus.ATRASADO, firstDayOfMonth, lastDayOfMonth);

        return new FinancialDashboardDTO(recebidoNoMes.subtract(pagoNoMes), aReceber, aPagar, inadimplencia);
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
                    .type(FinancialType.RECEITA)
                    .category(FinancialCategory.ALUGUEL)
                    .description("Aluguel - parcela " + (i + 1) + "/12")
                    .value(monthlyValue)
                    .dueDate(startDate.plusMonths(i))
                    .status(FinancialStatus.PENDENTE)
                    .recurrent(true)
                    .build();
            financialRepository.save(entry);
        }
        log.info("12 parcelas de aluguel geradas para o contrato {}: valor mensal={}", contractId, monthlyValue);
    }

    @Transactional
    public void createSingleEntry(UUID contractId, FinancialType type, FinancialCategory category,
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
                .status(FinancialStatus.PENDENTE)
                .recurrent(false)
                .build();
        financialRepository.save(entry);
    }

    private FinancialEntry findOwned(UUID id) {
        return financialRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Lancamento financeiro", id));
    }
}
