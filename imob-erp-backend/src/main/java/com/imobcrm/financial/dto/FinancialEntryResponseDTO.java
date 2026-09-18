package com.imobcrm.financial.dto;

import com.imobcrm.financial.FinancialCategory;
import com.imobcrm.financial.FinancialStatus;
import com.imobcrm.financial.FinancialType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialEntryResponseDTO(
        UUID id,
        UUID contractId,
        FinancialType type,
        FinancialCategory category,
        String description,
        BigDecimal value,
        LocalDate dueDate,
        LocalDate paidAt,
        FinancialStatus status,
        boolean recurrent
) {
}
