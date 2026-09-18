package com.imobcrm.property.dto;

import com.imobcrm.property.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusUpdateDTO(@NotNull PropertyStatus status) {
}
