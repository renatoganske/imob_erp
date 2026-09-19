package com.imobcrm.property.api;

import com.imobcrm.property.domain.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusRequest(@NotNull PropertyStatus status) {
}
