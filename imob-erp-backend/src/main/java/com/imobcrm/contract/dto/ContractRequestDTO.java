package com.imobcrm.contract.dto;

import com.imobcrm.contract.AdjustmentIndex;
import com.imobcrm.contract.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractRequestDTO(
        UUID leadId,
        @NotNull UUID propertyId,
        @NotNull UUID agentId,
        @NotNull ContractType type,
        @NotNull @Positive BigDecimal value,
        LocalDate signedAt,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        AdjustmentIndex adjustmentIndex,
        @NotBlank String buyerName,
        @NotBlank String buyerDocument,
        @NotBlank String ownerName,
        @NotBlank String ownerDocument,
        BigDecimal commissionRateOverride,
        String notes
) {
}
