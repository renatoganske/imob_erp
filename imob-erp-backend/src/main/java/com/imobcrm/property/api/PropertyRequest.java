package com.imobcrm.property.api;

import com.imobcrm.property.domain.enums.PropertyPurpose;
import com.imobcrm.property.domain.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PropertyRequest(
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
