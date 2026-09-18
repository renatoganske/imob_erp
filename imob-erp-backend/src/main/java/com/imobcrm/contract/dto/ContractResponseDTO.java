package com.imobcrm.contract.dto;

import com.imobcrm.contract.AdjustmentIndex;
import com.imobcrm.contract.ContractStatus;
import com.imobcrm.contract.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponseDTO(
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
}
