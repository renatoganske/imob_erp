package com.imobcrm.commission.domain;

import com.imobcrm.commission.api.CommissionReportResponse;
import com.imobcrm.commission.api.CommissionResponse;
import com.imobcrm.commission.domain.enums.CommissionStatus;
import com.imobcrm.commission.infra.CommissionMapper;
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
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    // O PostgreSQL nao infere o tipo de um OffsetDateTime nulo em "(:from IS NULL OR ...)", entao o
    // periodo aberto e representado por limites extremos em vez de parametros nulos.
    private static final OffsetDateTime PERIOD_MIN = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime PERIOD_MAX = OffsetDateTime.parse("9999-12-31T23:59:59Z");

    private final CommissionRepository commissionRepository;
    private final CommissionMapper commissionMapper;

    @Transactional(readOnly = true)
    public Page<CommissionResponse> search(UUID agentId, CommissionStatus status,
                                                OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return commissionRepository.search(TenantContext.tenantId(), agentId, status, orMin(from), orMax(to), pageable)
                .map(commissionMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CommissionResponse findById(UUID id) {
        return commissionMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public CommissionResponse pay(UUID id) {
        Commission commission = findOwned(id);
        if (commission.getStatus() == CommissionStatus.PAGO) {
            throw new BusinessException("Comissao ja esta paga", "COMMISSION_ALREADY_PAID");
        }
        commission.setStatus(CommissionStatus.PAGO);
        commission.setPaidAt(LocalDate.now());
        log.info("Comissao {} paga: agentId={} value={}", id, commission.getAgentId(), commission.getValue());
        return commissionMapper.toResponseDTO(commissionRepository.save(commission));
    }

    @Transactional(readOnly = true)
    public java.util.List<CommissionReportResponse> report(OffsetDateTime from, OffsetDateTime to) {
        return commissionRepository.reportByAgent(TenantContext.tenantId(), orMin(from), orMax(to)).stream()
                .map(row -> new CommissionReportResponse(row.getAgentId(), row.getTotal()))
                .toList();
    }

    /**
     * RN-08/RN-09: comissao = contract.value * (rateOverride ou agent.commissionRate) / 100.
     * Chamado pelo ContractService#activate.
     */
    @Transactional
    public void createFromContract(UUID contractId, UUID agentId, BigDecimal baseValue, BigDecimal rate) {
        if (rate == null) {
            throw new BusinessException("Taxa de comissao nao definida para o corretor", "AGENT_WITHOUT_COMMISSION_RATE");
        }
        BigDecimal value = baseValue.multiply(rate).divide(BigDecimal.valueOf(100));
        Commission commission = Commission.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .contractId(contractId)
                .agentId(agentId)
                .rate(rate)
                .baseValue(baseValue)
                .value(value)
                .status(CommissionStatus.PENDENTE)
                .build();
        commissionRepository.save(commission);
        log.info("Comissao gerada a partir do contrato {}: agentId={} rate={}% value={}",
                contractId, agentId, rate, value);
    }

    private Commission findOwned(UUID id) {
        return commissionRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Comissao", id));
    }

    private static OffsetDateTime orMin(OffsetDateTime value) {
        return value != null ? value : PERIOD_MIN;
    }

    private static OffsetDateTime orMax(OffsetDateTime value) {
        return value != null ? value : PERIOD_MAX;
    }
}
