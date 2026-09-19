package com.imobcrm.commission.infra;

import com.imobcrm.commission.domain.Commission;
import com.imobcrm.commission.domain.CommissionRepository;
import com.imobcrm.commission.domain.enums.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCommissionRepository extends JpaRepository<Commission, UUID>, CommissionRepository {

    Optional<Commission> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT c FROM Commission c
            WHERE c.tenantId = :tenantId
              AND (:agentId IS NULL OR c.agentId = :agentId)
              AND (:status IS NULL OR c.status = :status)
              AND (:from IS NULL OR c.createdAt >= :from)
              AND (:to IS NULL OR c.createdAt <= :to)
            """)
    Page<Commission> search(
            @Param("tenantId") UUID tenantId,
            @Param("agentId") UUID agentId,
            @Param("status") CommissionStatus status,
            @Param("from") java.time.OffsetDateTime from,
            @Param("to") java.time.OffsetDateTime to,
            Pageable pageable);

    @Query("""
            SELECT c.agentId AS agentId, SUM(c.value) AS total FROM Commission c
            WHERE c.tenantId = :tenantId
              AND (:from IS NULL OR c.createdAt >= :from)
              AND (:to IS NULL OR c.createdAt <= :to)
            GROUP BY c.agentId
            """)
    List<CommissionRepository.CommissionAgentTotal> reportByAgent(
            @Param("tenantId") UUID tenantId,
            @Param("from") java.time.OffsetDateTime from,
            @Param("to") java.time.OffsetDateTime to);
}
