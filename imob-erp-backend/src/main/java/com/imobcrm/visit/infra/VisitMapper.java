package com.imobcrm.visit.infra;

import com.imobcrm.visit.api.VisitResponse;
import com.imobcrm.visit.domain.Visit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    VisitResponse toResponseDTO(Visit visit);
}
