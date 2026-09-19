package com.imobcrm.financial.api;

import com.imobcrm.financial.domain.enums.EntryCategory;
import com.imobcrm.financial.domain.enums.EntryStatus;
import com.imobcrm.financial.domain.enums.EntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialEntryResponse(
        UUID id,
        UUID contractId,
        EntryType type,
        EntryCategory category,
        String description,
        BigDecimal value,
        LocalDate dueDate,
        LocalDate paidAt,
        EntryStatus status,
        boolean recurrent
) {
}
