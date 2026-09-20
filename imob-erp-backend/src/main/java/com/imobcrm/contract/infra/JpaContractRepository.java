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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface JpaContractRepository extends JpaRepository<Contract, UUID>, ContractRepository {

    Optional<Contract> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT c FROM Contract c
            WHERE c.tenantId = :tenantId
              AND (:agentId IS NULL OR c.agentId = :agentId)
              AND (:status IS NULL OR c.status = :status)
              AND (:type IS NULL OR c.type = :type)
              AND (CAST(:endFrom AS LocalDate) IS NULL OR c.endDate >= :endFrom)
              AND (CAST(:endTo AS LocalDate) IS NULL OR c.endDate <= :endTo)
            """)
    Page<Contract> search(@Param("tenantId") UUID tenantId, @Param("agentId") UUID agentId,
                           @Param("status") ContractStatus status, @Param("type") ContractType type,
                           @Param("endFrom") LocalDate endFrom, @Param("endTo") LocalDate endTo,
                           Pageable pageable);

    @Query("""
            SELECT COUNT(c) FROM Contract c
            WHERE c.tenantId = :tenantId
              AND c.status = com.imobcrm.contract.domain.enums.ContractStatus.ATIVO
              AND c.type = com.imobcrm.contract.domain.enums.ContractType.LOCACAO
              AND c.endDate BETWEEN :from AND :to
            """)
    long countActiveRentalsEndingBetween(@Param("tenantId") UUID tenantId,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to);
}
