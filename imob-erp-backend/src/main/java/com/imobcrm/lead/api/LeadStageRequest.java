package com.imobcrm.lead.api;

import com.imobcrm.lead.domain.enums.LeadStage;
import jakarta.validation.constraints.NotNull;

public record LeadStageRequest(@NotNull LeadStage stage) {
}
