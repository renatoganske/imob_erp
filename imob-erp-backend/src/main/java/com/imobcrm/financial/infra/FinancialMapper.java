package com.imobcrm.financial.infra;

import com.imobcrm.financial.api.FinancialEntryResponse;
import com.imobcrm.financial.domain.FinancialEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FinancialMapper {

    FinancialEntryResponse toResponseDTO(FinancialEntry entry);
}
