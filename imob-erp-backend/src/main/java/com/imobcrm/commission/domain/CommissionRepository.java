package com.imobcrm.commission.domain;

import com.imobcrm.commission.domain.enums.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommissionRepository {

    <S extends Commission> S save(S entity);

    Optional<Commission> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Commission> search(
            UUID tenantId,
            UUID agentId,
            CommissionStatus status,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            Pageable pageable);

    List<CommissionAgentTotal> reportByAgent(
            UUID tenantId,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to);

    interface CommissionAgentTotal {
        UUID getAgentId();
        java.math.BigDecimal getTotal();
    }
}
