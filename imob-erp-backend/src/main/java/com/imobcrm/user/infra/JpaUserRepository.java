package com.imobcrm.user.infra;

import com.imobcrm.user.domain.User;
import com.imobcrm.user.domain.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {

    Optional<User> findByClerkUserId(String clerkUserId);

    List<User> findAllByTenantId(UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}
