package com.imobcrm.property.infra;

import com.imobcrm.property.domain.Property;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.property.domain.enums.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface JpaPropertyRepository extends JpaRepository<Property, UUID>, PropertyRepository {

    Optional<Property> findByIdAndTenantIdAndActiveTrue(UUID id, UUID tenantId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id AND p.tenantId = :tenantId AND p.active = true")
    Optional<Property> lockByIdAndTenantIdAndActiveTrue(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("""
            SELECT p FROM Property p
            WHERE p.tenantId = :tenantId
              AND p.active = true
              AND (:type IS NULL OR p.type = :type)
              AND (:status IS NULL OR p.status = :status)
              AND (:neighborhood IS NULL OR p.neighborhood = :neighborhood)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Property> search(
            @Param("tenantId") UUID tenantId,
            @Param("type") PropertyType type,
            @Param("status") PropertyStatus status,
            @Param("neighborhood") String neighborhood,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
