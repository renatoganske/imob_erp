package com.imobcrm.lead.domain;

import com.imobcrm.lead.domain.enums.LeadStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface LeadRepository {

    <S extends Lead> S save(S entity);

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    Page<Lead> search(UUID tenantId, UUID assignedTo,
                       LeadStage stage, Pageable pageable);
}
