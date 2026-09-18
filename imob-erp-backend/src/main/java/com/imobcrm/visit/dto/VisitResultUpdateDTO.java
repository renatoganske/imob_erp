package com.imobcrm.visit.dto;

import jakarta.validation.constraints.NotBlank;

public record VisitResultUpdateDTO(@NotBlank String result) {
}
