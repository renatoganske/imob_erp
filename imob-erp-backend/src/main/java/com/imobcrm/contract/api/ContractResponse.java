package com.imobcrm.contract.api;

import com.imobcrm.contract.domain.enums.AdjustmentIndex;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID leadId,
        UUID propertyId,
        UUID agentId,
        ContractType type,
        ContractStatus status,
        BigDecimal value,
        LocalDate signedAt,
        LocalDate startDate,
        LocalDate endDate,
        AdjustmentIndex adjustmentIndex,
        String buyerName,
        String buyerDocument,
        String ownerName,
        String ownerDocument,
        String documentUrl,
        BigDecimal commissionRateOverride,
        String notes
) {

    /**
     * Copia sem os dados sensiveis (CPF/CNPJ das partes e URL do PDF assinado), usada quando o
     * leitor e o corretor responsavel: ele acompanha o contrato, mas nao recebe documentos pessoais.
     */
    public ContractResponse withoutSensitiveData() {
        return new ContractResponse(id, leadId, propertyId, agentId, type, status, value, signedAt, startDate,
                endDate, adjustmentIndex, buyerName, null, ownerName, null, null, commissionRateOverride, notes);
    }
}
