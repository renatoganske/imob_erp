package com.imobcrm.contract.dto;

import com.imobcrm.contract.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusUpdateDTO(@NotNull ContractStatus status) {
}
