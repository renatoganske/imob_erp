package com.imobcrm.financial.dto;

import com.imobcrm.financial.FinancialCategory;
import com.imobcrm.financial.FinancialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialEntryRequestDTO(
        @NotNull FinancialType type,
        @NotNull FinancialCategory category,
        @NotBlank String description,
        @NotNull @Positive BigDecimal value,
        @NotNull LocalDate dueDate
) {
}
