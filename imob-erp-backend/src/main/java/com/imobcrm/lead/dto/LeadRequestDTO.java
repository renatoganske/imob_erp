package com.imobcrm.lead.dto;

import com.imobcrm.lead.LeadSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeadRequestDTO(
        @NotBlank String name,
        @NotBlank String phone,
        String email,
        @NotNull LeadSource source,
        UUID assignedTo,
        String notes
) {
}
