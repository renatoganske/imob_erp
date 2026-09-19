package com.imobcrm.visit.api;

import jakarta.validation.constraints.NotBlank;

public record VisitResultRequest(@NotBlank String result) {
}
