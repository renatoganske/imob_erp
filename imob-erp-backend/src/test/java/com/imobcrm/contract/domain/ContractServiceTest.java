package com.imobcrm.contract.domain;

import com.imobcrm.contract.api.ContractResponse;
import com.imobcrm.contract.api.ExpiringContractsSummary;
import org.mockito.Spy;
import org.springframework.data.domain.Sort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import com.imobcrm.commission.domain.CommissionService;
import com.imobcrm.contract.api.ContractRequest;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import com.imobcrm.contract.infra.ContractMapper;
import com.imobcrm.financial.domain.FinancialService;
import com.imobcrm.lead.domain.LeadRepository;
import com.imobcrm.property.domain.Property;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.storage.UploadValidator;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.User;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private UploadValidator uploadValidator;
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-09-20T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

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

    @Test
    void activate_isBlockedWhenAgentHasNoCommissionRate() {
        Contract draft = draftContract(null);
        when(contractRepository.findByIdAndTenantId(draft.getId(), tenantId)).thenReturn(Optional.of(draft));
        when(userRepository.findByIdAndTenantId(agentId, tenantId))
                .thenReturn(Optional.of(User.builder().id(agentId).name("Ana").commissionRate(null).build()));

        assertThatThrownBy(() -> contractService.updateStatus(draft.getId(), ContractStatus.ATIVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ana")
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("AGENT_WITHOUT_COMMISSION_RATE");

        assertThat(draft.getStatus()).isEqualTo(ContractStatus.RASCUNHO);
        verify(propertyRepository, never()).save(any());
        verify(financialService, never()).createSingleEntry(any(), any(), any(), any(), any(), any());
        verify(commissionService, never()).createFromContract(any(), any(), any(), any());
    }

    @Test
    void activate_proceedsWhenContractOverridesTheRate() {
        Contract draft = draftContract(new BigDecimal("3"));
        when(contractRepository.findByIdAndTenantId(draft.getId(), tenantId)).thenReturn(Optional.of(draft));
        when(userRepository.findByIdAndTenantId(agentId, tenantId))
                .thenReturn(Optional.of(User.builder().id(agentId).name("Ana").commissionRate(null).build()));
        when(propertyRepository.findByIdAndTenantIdAndActiveTrue(propertyId, tenantId))
                .thenReturn(Optional.of(Property.builder().id(propertyId).build()));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        contractService.updateStatus(draft.getId(), ContractStatus.ATIVO);

        assertThat(draft.getStatus()).isEqualTo(ContractStatus.ATIVO);
        verify(commissionService).createFromContract(draft.getId(), agentId, draft.getValue(), new BigDecimal("3"));
    }

    private Contract draftContract(BigDecimal rateOverride) {
        return Contract.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .propertyId(propertyId)
                .agentId(agentId)
                .type(ContractType.COMPRA_VENDA)
                .status(ContractStatus.RASCUNHO)
                .value(new BigDecimal("500000"))
                .startDate(LocalDate.now())
                .commissionRateOverride(rateOverride)
                .build();
    }

    // ---- leitura pelo corretor (IMOB-35) ----

    private Contract contractOf(UUID agent) {
        return Contract.builder().id(UUID.randomUUID()).tenantId(tenantId).agentId(agent).build();
    }

    private ContractResponse fullResponse(Contract c) {
        return new ContractResponse(c.getId(), null, propertyId, c.getAgentId(), ContractType.COMPRA_VENDA, null,
                new BigDecimal("500000"), null, LocalDate.now(), null, null, "Comprador", "111", "Dono", "222",
                "http://r2/doc.pdf", null, "obs");
    }

    private void loginAs(Role role, UUID userId) {
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, userId, "clerk_" + userId, role));
    }

    @Test
    void search_forCorretorScopesToOwnContractsAndMasksSensitiveData() {
        UUID corretorId = UUID.randomUUID();
        loginAs(Role.CORRETOR, corretorId);
        Contract own = contractOf(corretorId);
        Pageable pageable = PageRequest.of(0, 20);
        when(contractRepository.search(tenantId, corretorId, null, null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(own)));
        when(contractMapper.toResponseDTO(own)).thenReturn(fullResponse(own));

        ContractResponse result = contractService.search(null, null, null, pageable).getContent().get(0);

        assertThat(result.buyerName()).isEqualTo("Comprador");
        assertThat(result.buyerDocument()).isNull();
        assertThat(result.ownerDocument()).isNull();
        assertThat(result.documentUrl()).isNull();
    }

    @Test
    void search_forAdminAndFinanceiroIsNotScopedByAgentAndKeepsSensitiveData() {
        for (Role role : new Role[]{Role.ADMIN, Role.FINANCEIRO}) {
            loginAs(role, UUID.randomUUID());
            Contract any = contractOf(UUID.randomUUID());
            Pageable pageable = PageRequest.of(0, 20);
            when(contractRepository.search(tenantId, null, null, null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(any)));
            when(contractMapper.toResponseDTO(any)).thenReturn(fullResponse(any));

            ContractResponse result = contractService.search(null, null, null, pageable).getContent().get(0);

            assertThat(result.buyerDocument()).isEqualTo("111");
            assertThat(result.documentUrl()).isEqualTo("http://r2/doc.pdf");
        }
    }

    @Test
    void findById_forCorretorHidesAnotherAgentsContractAsNotFound() {
        loginAs(Role.CORRETOR, UUID.randomUUID());
        Contract foreign = contractOf(UUID.randomUUID());
        when(contractRepository.findByIdAndTenantId(foreign.getId(), tenantId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> contractService.findById(foreign.getId())).isInstanceOf(ResourceNotFoundException.class);
        verify(contractMapper, never()).toResponseDTO(any());
    }

    @Test
    void findById_forCorretorReturnsOwnContractWithoutSensitiveData() {
        UUID corretorId = UUID.randomUUID();
        loginAs(Role.CORRETOR, corretorId);
        Contract own = contractOf(corretorId);
        when(contractRepository.findByIdAndTenantId(own.getId(), tenantId)).thenReturn(Optional.of(own));
        when(contractMapper.toResponseDTO(own)).thenReturn(fullResponse(own));

        ContractResponse result = contractService.findById(own.getId());

        assertThat(result.buyerDocument()).isNull();
        assertThat(result.documentUrl()).isNull();
        assertThat(result.notes()).isEqualTo("obs");
    }

    // ---- alertas de vencimento (IMOB-28); "hoje" = 2026-09-20 no fuso de Brasilia ----

    @Test
    void search_withExpiringInDaysRestrictsToActiveRentalsInWindowSortedBySoonestEnd() {
        when(contractRepository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        contractService.search(null, null, 30, PageRequest.of(0, 20));

        verify(contractRepository).search(tenantId, null, ContractStatus.ATIVO, ContractType.LOCACAO,
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 20), PageRequest.of(0, 20, Sort.by("endDate")));
    }

    @Test
    void search_withExpiringInDaysKeepsAnExplicitSort() {
        Pageable byValue = PageRequest.of(1, 10, Sort.by("value"));
        when(contractRepository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        contractService.search(null, ContractType.LOCACAO, 90, byValue);

        verify(contractRepository).search(tenantId, null, ContractStatus.ATIVO, ContractType.LOCACAO,
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 12, 19), byValue);
    }

    @Test
    void search_withExpiringInDaysRejectsAConflictingStatusOrType() {
        assertThatThrownBy(() -> contractService.search(ContractStatus.ENCERRADO, null, 30, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_EXPIRY_FILTER"));
        assertThatThrownBy(() -> contractService.search(null, ContractType.COMPRA_VENDA, 30, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_EXPIRY_FILTER"));
        verify(contractRepository, never()).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void search_withExpiringInDaysOutOfRangeIsRejected() {
        for (int days : new int[]{0, -5, 366}) {
            assertThatThrownBy(() -> contractService.search(null, null, days, PageRequest.of(0, 20)))
                    .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_EXPIRY_WINDOW"));
        }
    }

    @Test
    void search_withoutExpiringInDaysDoesNotFilterByEndDate() {
        Pageable pageable = PageRequest.of(0, 20);
        when(contractRepository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        contractService.search(ContractStatus.ATIVO, null, null, pageable);

        verify(contractRepository).search(tenantId, null, ContractStatus.ATIVO, null, null, null, pageable);
    }

    @Test
    void expiringSummary_countsActiveRentalsEndingWithin30_60And90Days() {
        LocalDate today = LocalDate.of(2026, 9, 20);
        when(contractRepository.countActiveRentalsEndingBetween(tenantId, today, today.plusDays(30))).thenReturn(1L);
        when(contractRepository.countActiveRentalsEndingBetween(tenantId, today, today.plusDays(60))).thenReturn(3L);
        when(contractRepository.countActiveRentalsEndingBetween(tenantId, today, today.plusDays(90))).thenReturn(5L);

        assertThat(contractService.expiringSummary()).isEqualTo(new ExpiringContractsSummary(1, 3, 5));
    }

    private ContractRequest request(UUID lead) {
        return new ContractRequest(lead, propertyId, agentId, ContractType.COMPRA_VENDA, new BigDecimal("500000"),
                null, LocalDate.now(), null, null, "Comprador", "111", "Dono", "222", null, null);
    }
}
