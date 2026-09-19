package com.imobcrm.visit.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitRequest(
        @NotNull UUID leadId,
        @NotNull UUID propertyId,
        @NotNull UUID agentId,
        @NotNull @Future OffsetDateTime scheduledAt
) {
}
