package com.imobcrm.visit.api;

import com.imobcrm.visit.domain.enums.VisitStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID leadId,
        UUID propertyId,
        UUID agentId,
        OffsetDateTime scheduledAt,
        VisitStatus status,
        String result
) {
}
