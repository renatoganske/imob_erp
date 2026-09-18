package com.imobcrm.commission.dto;

import com.imobcrm.commission.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommissionResponseDTO(
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
