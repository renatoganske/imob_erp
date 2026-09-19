package com.imobcrm.contract.infra;

import com.imobcrm.contract.api.ContractRequest;
import com.imobcrm.contract.api.ContractResponse;
import com.imobcrm.contract.domain.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContractMapper {

    ContractResponse toResponseDTO(Contract contract);

    void updateFromRequest(ContractRequest request, @MappingTarget Contract contract);
}
