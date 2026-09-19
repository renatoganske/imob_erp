package com.imobcrm.contract.infra;

import com.imobcrm.contract.domain.Contract;
import com.imobcrm.contract.domain.ContractRepository;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaContractRepository extends JpaRepository<Contract, UUID>, ContractRepository {

    Optional<Contract> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT c FROM Contract c
            WHERE c.tenantId = :tenantId
              AND (:status IS NULL OR c.status = :status)
              AND (:type IS NULL OR c.type = :type)
            """)
    Page<Contract> search(@Param("tenantId") UUID tenantId, @Param("status") ContractStatus status,
                           @Param("type") ContractType type, Pageable pageable);
}
