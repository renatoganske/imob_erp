package com.imobcrm.lead.dto;

import com.imobcrm.lead.LeadStage;
import jakarta.validation.constraints.NotNull;

public record LeadStageUpdateDTO(@NotNull LeadStage stage) {
}
