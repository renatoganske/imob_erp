package com.imobcrm.visit.domain;

import com.imobcrm.lead.domain.LeadRepository;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import com.imobcrm.visit.api.VisitRequest;
import com.imobcrm.visit.infra.VisitMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock private VisitRepository visitRepository;
    @Mock private VisitMapper visitMapper;
    @Mock private LeadRepository leadRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private VisitService visitService;

    private final UUID tenantId = UUID.randomUUID();
    private final VisitRequest request = new VisitRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(1));

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, UUID.randomUUID(), "clerk_1", Role.ADMIN));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_rejectsLeadFromAnotherTenant() {
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(false);

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_rejectsPropertyFromAnotherTenant() {
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)).thenReturn(false);

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_rejectsAgentFromAnotherTenant() {
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)).thenReturn(true);
        when(userRepository.existsByIdAndTenantId(request.agentId(), tenantId)).thenReturn(false);

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_persistsWhenAllReferencesBelongToTenant() {
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)).thenReturn(true);
        when(userRepository.existsByIdAndTenantId(request.agentId(), tenantId)).thenReturn(true);
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));

        visitService.create(request);

        verify(visitRepository).save(any(Visit.class));
    }
}
