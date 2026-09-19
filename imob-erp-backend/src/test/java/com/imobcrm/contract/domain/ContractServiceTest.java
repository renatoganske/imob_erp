package com.imobcrm.contract.domain;

import com.imobcrm.commission.domain.CommissionService;
import com.imobcrm.contract.api.ContractRequest;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import com.imobcrm.contract.infra.ContractMapper;
import com.imobcrm.financial.domain.FinancialService;
import com.imobcrm.lead.domain.LeadRepository;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock private ContractRepository contractRepository;
    @Mock private ContractMapper contractMapper;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private FinancialService financialService;
    @Mock private CommissionService commissionService;
    @Mock private R2StorageService storageService;

    @InjectMocks private ContractService contractService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID leadId = UUID.randomUUID();
    private final UUID propertyId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();

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
        when(leadRepository.existsByIdAndTenantId(leadId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> contractService.create(request(leadId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void create_rejectsPropertyFromAnotherTenant() {
        when(leadRepository.existsByIdAndTenantId(leadId, tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(propertyId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> contractService.create(request(leadId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void create_rejectsAgentFromAnotherTenant() {
        when(leadRepository.existsByIdAndTenantId(leadId, tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(propertyId, tenantId)).thenReturn(true);
        when(userRepository.existsByIdAndTenantId(agentId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> contractService.create(request(leadId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void create_persistsWhenAllReferencesBelongToTenant() {
        when(leadRepository.existsByIdAndTenantId(leadId, tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(propertyId, tenantId)).thenReturn(true);
        when(userRepository.existsByIdAndTenantId(agentId, tenantId)).thenReturn(true);
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        contractService.create(request(leadId));

        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    void create_allowsContractWithoutLead() {
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(propertyId, tenantId)).thenReturn(true);
        when(userRepository.existsByIdAndTenantId(agentId, tenantId)).thenReturn(true);
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        contractService.create(request(null));

        verify(leadRepository, never()).existsByIdAndTenantId(any(), any());
    }

    @Test
    void update_rejectsPropertyFromAnotherTenant() {
        UUID contractId = UUID.randomUUID();
        Contract draft = Contract.builder().id(contractId).tenantId(tenantId).status(ContractStatus.RASCUNHO).build();
        when(contractRepository.findByIdAndTenantId(contractId, tenantId)).thenReturn(Optional.of(draft));
        when(leadRepository.existsByIdAndTenantId(leadId, tenantId)).thenReturn(true);
        when(propertyRepository.existsByIdAndTenantIdAndActiveTrue(propertyId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> contractService.update(contractId, request(leadId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(contractRepository, never()).save(any());
    }

    private ContractRequest request(UUID lead) {
        return new ContractRequest(lead, propertyId, agentId, ContractType.COMPRA_VENDA, new BigDecimal("500000"),
                null, LocalDate.now(), null, null, "Comprador", "111", "Dono", "222", null, null);
    }
}
