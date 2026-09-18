package com.imobcrm.financial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialRepository extends JpaRepository<FinancialEntry, UUID> {

    Optional<FinancialEntry> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT f FROM FinancialEntry f
            WHERE f.tenantId = :tenantId
              AND (:type IS NULL OR f.type = :type)
              AND (:status IS NULL OR f.status = :status)
              AND (:category IS NULL OR f.category = :category)
              AND (:from IS NULL OR f.dueDate >= :from)
              AND (:to IS NULL OR f.dueDate <= :to)
            """)
    Page<FinancialEntry> search(
            @Param("tenantId") UUID tenantId,
            @Param("type") FinancialType type,
            @Param("status") FinancialStatus status,
            @Param("category") FinancialCategory category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    List<FinancialEntry> findAllByTenantIdAndStatusAndDueDateBefore(UUID tenantId, FinancialStatus status, LocalDate date);

    List<FinancialEntry> findAllByStatusAndDueDateBefore(FinancialStatus status, LocalDate date);

    @Query("""
            SELECT COALESCE(SUM(f.value), 0) FROM FinancialEntry f
            WHERE f.tenantId = :tenantId AND f.type = :type AND f.status = :status
              AND f.dueDate BETWEEN :from AND :to
            """)
    java.math.BigDecimal sumByTypeAndStatusInPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("type") FinancialType type,
            @Param("status") FinancialStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
