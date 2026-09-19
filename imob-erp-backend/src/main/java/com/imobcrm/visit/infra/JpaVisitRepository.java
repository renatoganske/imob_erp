package com.imobcrm.visit.infra;

import com.imobcrm.visit.domain.Visit;
import com.imobcrm.visit.domain.VisitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaVisitRepository extends JpaRepository<Visit, UUID>, VisitRepository {

    Optional<Visit> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT v FROM Visit v
            WHERE v.tenantId = :tenantId
              AND (:agentId IS NULL OR v.agentId = :agentId)
            """)
    Page<Visit> search(@Param("tenantId") UUID tenantId, @Param("agentId") UUID agentId, Pageable pageable);
}
