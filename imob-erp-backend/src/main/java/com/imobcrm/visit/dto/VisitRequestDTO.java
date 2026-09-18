package com.imobcrm.visit.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitRequestDTO(
        @NotNull UUID leadId,
        @NotNull UUID propertyId,
        @NotNull UUID agentId,
        @NotNull @Future OffsetDateTime scheduledAt
) {
}
