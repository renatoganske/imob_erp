package com.imobcrm.contract.domain;

import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {

    <S extends Contract> S save(S entity);

    Optional<Contract> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Contract> search(UUID tenantId, ContractStatus status,
                           ContractType type, Pageable pageable);
}
