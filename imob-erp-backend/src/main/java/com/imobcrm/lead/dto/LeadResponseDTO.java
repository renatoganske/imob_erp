package com.imobcrm.lead.dto;

import com.imobcrm.lead.LeadSource;
import com.imobcrm.lead.LeadStage;

import java.util.List;
import java.util.UUID;

public record LeadResponseDTO(
        UUID id,
        String name,
        String phone,
        String email,
        LeadSource source,
        LeadStage stage,
        UUID assignedTo,
        String notes,
        List<UUID> propertiesOfInterest
) {
}
