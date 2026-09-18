package com.imobcrm.lead;

import com.imobcrm.lead.dto.LeadResponseDTO;
import com.imobcrm.property.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "propertiesOfInterest", expression = "java(mapPropertyIds(lead))")
    LeadResponseDTO toResponseDTO(Lead lead);

    default java.util.List<java.util.UUID> mapPropertyIds(Lead lead) {
        return lead.getPropertiesOfInterest().stream().map(Property::getId).toList();
    }
}
