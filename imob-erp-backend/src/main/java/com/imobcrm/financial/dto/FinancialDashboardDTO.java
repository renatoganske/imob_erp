package com.imobcrm.financial.dto;

import java.math.BigDecimal;

public record FinancialDashboardDTO(
        BigDecimal saldoDoMes,
        BigDecimal totalAReceber,
        BigDecimal totalAPagar,
        BigDecimal inadimplencia
) {
}
