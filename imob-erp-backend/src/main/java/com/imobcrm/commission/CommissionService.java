package com.imobcrm.commission;

import com.imobcrm.commission.dto.CommissionReportItemDTO;
import com.imobcrm.commission.dto.CommissionResponseDTO;
import com.imobcrm.shared.exception.BusinessRuleException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepository commissionRepository;
    private final CommissionMapper commissionMapper;

    @Transactional(readOnly = true)
    public Page<CommissionResponseDTO> search(UUID agentId, CommissionStatus status,
                                                OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return commissionRepository.search(TenantContext.tenantId(), agentId, status, from, to, pageable)
                .map(commissionMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CommissionResponseDTO findById(UUID id) {
        return commissionMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public CommissionResponseDTO pay(UUID id) {
        Commission commission = findOwned(id);
        if (commission.getStatus() == CommissionStatus.PAGO) {
            throw new BusinessRuleException("Comissao ja esta paga", "COMMISSION_ALREADY_PAID");
        }
        commission.setStatus(CommissionStatus.PAGO);
        commission.setPaidAt(LocalDate.now());
        return commissionMapper.toResponseDTO(commissionRepository.save(commission));
    }

    @Transactional(readOnly = true)
    public java.util.List<CommissionReportItemDTO> report(OffsetDateTime from, OffsetDateTime to) {
        return commissionRepository.reportByAgent(TenantContext.tenantId(), from, to).stream()
                .map(row -> new CommissionReportItemDTO(row.getAgentId(), row.getTotal()))
                .toList();
    }

    /**
     * RN-08/RN-09: comissao = contract.value * (rateOverride ou agent.commissionRate) / 100.
     * Chamado pelo ContractService#activate.
     */
    @Transactional
    public void createFromContract(UUID contractId, UUID agentId, BigDecimal baseValue, BigDecimal rate) {
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
    }

    private Commission findOwned(UUID id) {
        return commissionRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Comissao", id));
    }
}
