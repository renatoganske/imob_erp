package com.imobcrm.lead.api;

import com.imobcrm.lead.domain.enums.LeadSource;
import com.imobcrm.lead.domain.enums.LeadStage;
import java.util.List;
import java.util.UUID;

public record LeadResponse(
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
