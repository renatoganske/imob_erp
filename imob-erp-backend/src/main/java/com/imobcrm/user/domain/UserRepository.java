package com.imobcrm.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    <S extends User> S save(S entity);

    Optional<User> findByClerkUserId(String clerkUserId);

    List<User> findAllByTenantId(UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByEmailIgnoreCase(String email);
}
