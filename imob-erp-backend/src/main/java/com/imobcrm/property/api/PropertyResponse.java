package com.imobcrm.property.api;

import com.imobcrm.property.domain.enums.PropertyPurpose;
import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.property.domain.enums.PropertyType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(
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
