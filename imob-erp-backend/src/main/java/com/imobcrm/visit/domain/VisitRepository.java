package com.imobcrm.visit.domain;

import com.imobcrm.visit.domain.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepository {

    <S extends Visit> S save(S entity);

    Optional<Visit> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Periodo [from, to): os limites sao sempre informados (o servico usa extremos para periodo aberto). */
    Page<Visit> search(UUID tenantId, UUID agentId, VisitStatus status,
                       OffsetDateTime from, OffsetDateTime to, Pageable pageable);
}
