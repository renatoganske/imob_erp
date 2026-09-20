package com.imobcrm.visit.domain;

import com.imobcrm.lead.domain.LeadRepository;
import com.imobcrm.property.domain.Property;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ForbiddenException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import com.imobcrm.visit.api.VisitRequest;
import com.imobcrm.visit.domain.enums.VisitStatus;
import com.imobcrm.visit.infra.VisitMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private final UUID userId = UUID.randomUUID();
    private final VisitRequest request = new VisitRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(1));

    private void loginAs(Role role) {
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, userId, "clerk_1", role));
    }

    private Property property(PropertyStatus status) {
        return Property.builder().id(request.propertyId()).status(status).build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_rejectsLeadFromAnotherTenant() {
        loginAs(Role.ADMIN);
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(false);

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_rejectsPropertyFromAnotherTenant() {
        loginAs(Role.ADMIN);
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_rejectsSoldProperty() {
        loginAs(Role.ADMIN);
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId))
                .thenReturn(Optional.of(property(PropertyStatus.VENDIDO)));

        assertThatThrownBy(() -> visitService.create(request))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("PROPERTY_SOLD"));
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_rejectsAgentFromAnotherTenant() {
        loginAs(Role.ADMIN);
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId))
                .thenReturn(Optional.of(property(PropertyStatus.DISPONIVEL)));
        when(userRepository.existsByIdAndTenantId(request.agentId(), tenantId)).thenReturn(false);

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void create_persistsWhenAllReferencesBelongToTenant() {
        loginAs(Role.ADMIN);
        when(leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)).thenReturn(true);
        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId))
                .thenReturn(Optional.of(property(PropertyStatus.RESERVADO)));
        when(userRepository.existsByIdAndTenantId(request.agentId(), tenantId)).thenReturn(true);
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));

        visitService.create(request);

        verify(visitRepository).save(any(Visit.class));
    }

    @Test
    void create_corretorCannotScheduleForAnotherAgent() {
        loginAs(Role.CORRETOR);

        assertThatThrownBy(() -> visitService.create(request)).isInstanceOf(ForbiddenException.class);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void search_corretorIsForcedToOwnVisits() {
        loginAs(Role.CORRETOR);
        Pageable pageable = PageRequest.of(0, 20);
        when(visitRepository.search(eq(tenantId), eq(userId), any(), any(), any(), eq(pageable))).thenReturn(Page.empty());

        visitService.search(UUID.randomUUID(), null, null, null, pageable);

        verify(visitRepository).search(eq(tenantId), eq(userId), any(), any(), any(), eq(pageable));
    }

    @Test
    void search_adminKeepsRequestedAgentAndStatus() {
        loginAs(Role.ADMIN);
        UUID agent = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(visitRepository.search(eq(tenantId), eq(agent), eq(VisitStatus.AGENDADA), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        visitService.search(agent, VisitStatus.AGENDADA, null, null, pageable);

        verify(visitRepository).search(eq(tenantId), eq(agent), eq(VisitStatus.AGENDADA), any(), any(), eq(pageable));
    }

    @Test
    void search_periodIsInclusiveInBrasiliaTime() {
        loginAs(Role.ADMIN);
        Pageable pageable = PageRequest.of(0, 20);
        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        when(visitRepository.search(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        visitService.search(null, null, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6), pageable);

        verify(visitRepository).search(eq(tenantId), eq(null), eq(null), from.capture(), to.capture(), eq(pageable));
        assertThat(from.getValue()).isEqualTo(OffsetDateTime.parse("2026-10-05T00:00:00-03:00"));
        assertThat(to.getValue()).isEqualTo(OffsetDateTime.parse("2026-10-07T00:00:00-03:00"));
    }

    @Test
    void findById_corretorGetsNotFoundForAnotherAgentsVisit() {
        loginAs(Role.CORRETOR);
        UUID id = UUID.randomUUID();
        Visit foreign = Visit.builder().id(id).tenantId(tenantId).agentId(UUID.randomUUID()).build();
        when(visitRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> visitService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
