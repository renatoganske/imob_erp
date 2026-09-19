package com.imobcrm.user.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UserCommissionRateRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal commissionRate
) {
}
