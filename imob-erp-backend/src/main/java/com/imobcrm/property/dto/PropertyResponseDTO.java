package com.imobcrm.property.dto;

import com.imobcrm.property.PropertyPurpose;
import com.imobcrm.property.PropertyStatus;
import com.imobcrm.property.PropertyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PropertyResponseDTO(
        UUID id,
        PropertyType type,
        String title,
        String description,
        String address,
        String neighborhood,
        String city,
        BigDecimal price,
        BigDecimal area,
        Integer bedrooms,
        Integer bathrooms,
        Integer parkingSpots,
        PropertyStatus status,
        PropertyPurpose purpose,
        List<String> photos
) {
}
