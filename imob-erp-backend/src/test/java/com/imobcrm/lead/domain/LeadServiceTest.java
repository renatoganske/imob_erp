package com.imobcrm.lead.domain;

import com.imobcrm.contract.domain.ContractService;
import com.imobcrm.lead.api.LeadRequest;
import com.imobcrm.lead.domain.enums.LeadSource;
import com.imobcrm.lead.domain.enums.LeadStage;
import com.imobcrm.lead.infra.LeadMapper;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ForbiddenException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeadMapper leadMapper;
    @Mock private ContractService contractService;

    @InjectMocks private LeadService leadService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID corretorId = UUID.randomUUID();
    private final UUID otherAgentId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void loginAs(Role role, UUID userId) {
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, userId, "clerk_" + userId, role));
    }

    private Lead lead(UUID assignedTo, LeadStage stage) {
        return Lead.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .assignedTo(assignedTo)
                .name("Lead")
                .phone("41999999999")
                .source(LeadSource.SITE)
                .stage(stage)
                .build();
    }

    @Test
    void search_forCorretorIgnoresAssignedToFilterAndUsesOwnId() {
        loginAs(Role.CORRETOR, corretorId);
        Pageable pageable = PageRequest.of(0, 20);
        when(leadRepository.search(tenantId, corretorId, null, pageable)).thenReturn(new PageImpl<>(List.of()));

        leadService.search(otherAgentId, null, pageable);

        verify(leadRepository).search(tenantId, corretorId, null, pageable);
    }

    @Test
    void search_forAdminKeepsRequestedAssignedToFilter() {
        loginAs(Role.ADMIN, UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 20);
        when(leadRepository.search(tenantId, otherAgentId, null, pageable)).thenReturn(new PageImpl<>(List.of()));

        leadService.search(otherAgentId, null, pageable);

        verify(leadRepository).search(tenantId, otherAgentId, null, pageable);
    }

    @Test
    void search_forFinanceiroSeesAllLeadsWhenNoFilter() {
        loginAs(Role.FINANCEIRO, UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 20);
        when(leadRepository.search(tenantId, null, null, pageable)).thenReturn(new PageImpl<>(List.of()));

        leadService.search(null, null, pageable);

        verify(leadRepository).search(tenantId, null, null, pageable);
    }

    @Test
    void findById_forCorretorHidesLeadOfAnotherAgentAsNotFound() {
        loginAs(Role.CORRETOR, corretorId);
        Lead foreign = lead(otherAgentId, LeadStage.NOVO);
        when(leadRepository.findByIdAndTenantId(foreign.getId(), tenantId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> leadService.findById(foreign.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_forAdminReturnsLeadOfAnyAgent() {
        loginAs(Role.ADMIN, UUID.randomUUID());
        Lead foreign = lead(otherAgentId, LeadStage.NOVO);
        when(leadRepository.findByIdAndTenantId(foreign.getId(), tenantId)).thenReturn(Optional.of(foreign));

        leadService.findById(foreign.getId());

        verify(leadMapper).toResponseDTO(foreign);
    }

    @Test
    void create_forCorretorCannotAssignToAnotherAgent() {
        loginAs(Role.CORRETOR, corretorId);
        var request = new LeadRequest("Novo", "41999999999", null, LeadSource.SITE, otherAgentId, null);

        assertThatThrownBy(() -> leadService.create(request)).isInstanceOf(ForbiddenException.class);
        verify(leadRepository, never()).save(any());
    }

    @Test
    void update_forCorretorCannotReassignLeadToAnotherAgent() {
        loginAs(Role.CORRETOR, corretorId);
        Lead own = lead(corretorId, LeadStage.NOVO);
        when(leadRepository.findByIdAndTenantId(own.getId(), tenantId)).thenReturn(Optional.of(own));
        var request = new LeadRequest("Novo", "41999999999", null, LeadSource.SITE, otherAgentId, null);

        assertThatThrownBy(() -> leadService.update(own.getId(), request)).isInstanceOf(ForbiddenException.class);
        assertThat(own.getAssignedTo()).isEqualTo(corretorId);
        verify(leadRepository, never()).save(any());
    }

    @Test
    void updateStage_toFechadoWithoutPropertyIs422AndDoesNotChangeTheLead() {
        loginAs(Role.CORRETOR, corretorId);
        Lead own = lead(corretorId, LeadStage.PROPOSTA);
        when(leadRepository.findByIdAndTenantId(own.getId(), tenantId)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> leadService.updateStage(own.getId(), LeadStage.FECHADO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("imovel");
        assertThat(own.getStage()).isEqualTo(LeadStage.PROPOSTA);
        verify(leadRepository, never()).save(any());
        verify(contractService, never()).create(any());
    }

    @Test
    void updateStage_ofAnotherAgentsLeadIsNotFoundForCorretor() {
        loginAs(Role.CORRETOR, corretorId);
        Lead foreign = lead(otherAgentId, LeadStage.NOVO);
        when(leadRepository.findByIdAndTenantId(foreign.getId(), tenantId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> leadService.updateStage(foreign.getId(), LeadStage.EM_ATENDIMENTO))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(leadRepository, never()).save(any());
    }
}
