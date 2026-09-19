package com.imobcrm.financial.api;

import java.math.BigDecimal;

public record FinancialDashboardResponse(
        BigDecimal saldoDoMes,
        BigDecimal totalAReceber,
        BigDecimal totalAPagar,
        BigDecimal inadimplencia
) {
}
