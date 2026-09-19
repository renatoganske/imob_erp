package com.imobcrm.commission.infra;

import com.imobcrm.commission.api.CommissionResponse;
import com.imobcrm.commission.domain.Commission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommissionMapper {

    CommissionResponse toResponseDTO(Commission commission);
}
