package com.imobcrm.property.domain;

import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.property.infra.PropertyMapper;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private R2StorageService storageService;

    private PropertyService propertyService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PropertyMapper mapper = Mappers.getMapper(PropertyMapper.class);
        propertyService = new PropertyService(propertyRepository, mapper, storageService);
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, UUID.randomUUID(), "clerk_1", Role.ADMIN));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void delete_marksPropertyInactive_insteadOfRemovingIt() {
        Property property = Property.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .status(PropertyStatus.DISPONIVEL)
                .active(true)
                .build();

        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(property.getId(), tenantId))
                .thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        propertyService.delete(property.getId());

        assertThat(property.isActive()).isFalse();
    }

    @Test
    void findById_throwsResourceNotFound_whenPropertyBelongsToAnotherTenant() {
        UUID id = UUID.randomUUID();
        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
