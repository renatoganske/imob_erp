package com.imobcrm.financial;

import com.imobcrm.financial.dto.FinancialEntryResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FinancialMapper {

    FinancialEntryResponseDTO toResponseDTO(FinancialEntry entry);
}
