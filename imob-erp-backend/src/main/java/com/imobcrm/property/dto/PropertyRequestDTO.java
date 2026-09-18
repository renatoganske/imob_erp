package com.imobcrm.property.dto;

import com.imobcrm.property.PropertyPurpose;
import com.imobcrm.property.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PropertyRequestDTO(
        @NotNull PropertyType type,
        @NotBlank String title,
        String description,
        @NotBlank String address,
        String neighborhood,
        @NotBlank String city,
        @NotNull @Positive BigDecimal price,
        BigDecimal area,
        Integer bedrooms,
        Integer bathrooms,
        Integer parkingSpots,
        @NotNull PropertyPurpose purpose
) {
}
