package com.imobcrm.lead.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeadAssignRequest(@NotNull UUID agentId) {
}
