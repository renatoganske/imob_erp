package com.imobcrm.lead.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeadAssignDTO(@NotNull UUID agentId) {
}
