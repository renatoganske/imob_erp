package com.imobcrm.property;

import com.imobcrm.property.dto.PropertyRequestDTO;
import com.imobcrm.property.dto.PropertyResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PropertyMapper {

    PropertyResponseDTO toResponseDTO(Property property);

    void updateFromRequest(PropertyRequestDTO request, @MappingTarget Property property);
}
