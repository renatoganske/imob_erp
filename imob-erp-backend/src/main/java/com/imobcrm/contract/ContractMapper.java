package com.imobcrm.contract;

import com.imobcrm.contract.dto.ContractRequestDTO;
import com.imobcrm.contract.dto.ContractResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContractMapper {

    ContractResponseDTO toResponseDTO(Contract contract);

    void updateFromRequest(ContractRequestDTO request, @MappingTarget Contract contract);
}
