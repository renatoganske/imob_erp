package com.imobcrm.visit.api;

import com.imobcrm.visit.domain.enums.VisitStatus;
import jakarta.validation.constraints.NotNull;

public record VisitStatusRequest(@NotNull VisitStatus status) {
}
