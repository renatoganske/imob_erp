package com.imobcrm.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionReportItemDTO(UUID agentId, BigDecimal total) {
}
