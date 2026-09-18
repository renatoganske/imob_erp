package com.imobcrm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT l FROM Lead l
            WHERE l.tenantId = :tenantId
              AND (:assignedTo IS NULL OR l.assignedTo = :assignedTo)
              AND (:stage IS NULL OR l.stage = :stage)
            """)
    Page<Lead> search(@Param("tenantId") UUID tenantId, @Param("assignedTo") UUID assignedTo,
                       @Param("stage") LeadStage stage, Pageable pageable);
}
