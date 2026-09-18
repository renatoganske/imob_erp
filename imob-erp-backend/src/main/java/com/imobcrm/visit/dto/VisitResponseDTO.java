package com.imobcrm.visit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitResponseDTO(
        UUID id,
        UUID leadId,
        UUID propertyId,
        UUID agentId,
        OffsetDateTime scheduledAt,
        com.imobcrm.visit.VisitStatus status,
        String result
) {
}
