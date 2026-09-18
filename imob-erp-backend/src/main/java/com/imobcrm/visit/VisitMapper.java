package com.imobcrm.visit;

import com.imobcrm.visit.dto.VisitResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    VisitResponseDTO toResponseDTO(Visit visit);
}
