package com.imobcrm.commission;

import com.imobcrm.commission.dto.CommissionResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommissionMapper {

    CommissionResponseDTO toResponseDTO(Commission commission);
}
