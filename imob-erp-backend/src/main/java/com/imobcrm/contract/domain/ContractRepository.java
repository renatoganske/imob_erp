package com.imobcrm.contract.domain;

import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {

    <S extends Contract> S save(S entity);

    Optional<Contract> findByIdAndTenantId(UUID id, UUID tenantId);

    /** {@code endFrom}/{@code endTo} sao opcionais e inclusivos; com eles, contratos sem {@code end_date} ficam fora. */
    Page<Contract> search(UUID tenantId, UUID agentId, ContractStatus status, ContractType type,
                           LocalDate endFrom, LocalDate endTo, Pageable pageable);

    /** Locacoes ATIVAS com {@code end_date} entre as datas (inclusivas), para os alertas de vencimento. */
    long countActiveRentalsEndingBetween(UUID tenantId, LocalDate from, LocalDate to);
}
