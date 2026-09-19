package com.imobcrm.commission.api;

import com.imobcrm.commission.domain.enums.CommissionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommissionResponse(
        UUID id,
        UUID contractId,
        UUID agentId,
        BigDecimal rate,
        BigDecimal baseValue,
        BigDecimal value,
        CommissionStatus status,
        LocalDate paidAt
) {
}
