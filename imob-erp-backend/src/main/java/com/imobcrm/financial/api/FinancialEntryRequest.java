package com.imobcrm.financial.api;

import com.imobcrm.financial.domain.enums.EntryCategory;
import com.imobcrm.financial.domain.enums.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialEntryRequest(
        @NotNull EntryType type,
        @NotNull EntryCategory category,
        @NotBlank String description,
        @NotNull @Positive BigDecimal value,
        @NotNull LocalDate dueDate
) {
}
