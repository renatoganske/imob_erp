package com.imobcrm.lead.api;

import com.imobcrm.lead.domain.enums.LeadSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeadRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String email,
        @NotNull LeadSource source,
        UUID assignedTo,
        String notes
) {
}
