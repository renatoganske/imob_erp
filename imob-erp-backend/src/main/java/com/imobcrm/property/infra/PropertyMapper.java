package com.imobcrm.property.infra;

import com.imobcrm.property.api.PropertyRequest;
import com.imobcrm.property.api.PropertyResponse;
import com.imobcrm.property.domain.Property;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PropertyMapper {

    PropertyResponse toResponseDTO(Property property);

    void updateFromRequest(PropertyRequest request, @MappingTarget Property property);
}
