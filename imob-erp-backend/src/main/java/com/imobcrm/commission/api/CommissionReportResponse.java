package com.imobcrm.commission.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionReportResponse(UUID agentId, BigDecimal total) {
}
