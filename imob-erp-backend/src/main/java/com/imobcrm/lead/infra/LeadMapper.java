package com.imobcrm.lead.infra;

import com.imobcrm.lead.api.LeadResponse;
import com.imobcrm.lead.domain.Lead;
import com.imobcrm.property.domain.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "propertiesOfInterest", expression = "java(mapPropertyIds(lead))")
    LeadResponse toResponseDTO(Lead lead);

    default java.util.List<java.util.UUID> mapPropertyIds(Lead lead) {
        return lead.getPropertiesOfInterest().stream().map(Property::getId).toList();
    }
}
