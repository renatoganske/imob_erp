package com.imobcrm.property.domain;

import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.property.domain.enums.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository {

    <S extends Property> S save(S entity);

    Optional<Property> findByIdAndTenantIdAndActiveTrue(UUID id, UUID tenantId);

    boolean existsByIdAndTenantIdAndActiveTrue(UUID id, UUID tenantId);

    /**
     * Busca o imovel ativo do tenant travando a linha ate o fim da transacao (SELECT ... FOR UPDATE).
     * Serializa escritas concorrentes sobre a lista de fotos, que o Hibernate regrava por inteiro.
     */
    Optional<Property> lockByIdAndTenantIdAndActiveTrue(UUID id, UUID tenantId);

    Page<Property> search(
            UUID tenantId,
            PropertyType type,
            PropertyStatus status,
            String neighborhood,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);
}
