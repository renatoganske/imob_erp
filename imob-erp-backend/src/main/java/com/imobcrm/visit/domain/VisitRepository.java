package com.imobcrm.visit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface VisitRepository {

    <S extends Visit> S save(S entity);

    Optional<Visit> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Visit> search(UUID tenantId, UUID agentId, Pageable pageable);
}
