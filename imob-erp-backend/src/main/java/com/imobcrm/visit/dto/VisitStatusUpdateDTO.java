package com.imobcrm.visit.dto;

import com.imobcrm.visit.VisitStatus;
import jakarta.validation.constraints.NotNull;

public record VisitStatusUpdateDTO(@NotNull VisitStatus status) {
}
