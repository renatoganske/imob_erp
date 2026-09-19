package com.imobcrm.contract.api;

import com.imobcrm.contract.domain.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusRequest(@NotNull ContractStatus status) {
}
