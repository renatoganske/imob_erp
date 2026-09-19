package com.imobcrm.commission.domain;

import com.imobcrm.commission.infra.CommissionMapper;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionRepository commissionRepository;
    @Mock private CommissionMapper commissionMapper;

    @InjectMocks private CommissionService commissionService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, UUID.randomUUID(), "clerk_1", Role.ADMIN));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createFromContract_skipsWhenAgentHasNoRate() {
        commissionService.createFromContract(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("460000.00"), null);

        verify(commissionRepository, never()).save(any());
    }

    @Test
    void createFromContract_computesValueFromRate() {
        commissionService.createFromContract(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("460000.00"), new BigDecimal("5"));

        ArgumentCaptor<Commission> saved = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(saved.capture());
        assertThat(saved.getValue().getValue()).isEqualByComparingTo("23000");
    }

    @Test
    void search_neverPassesNullPeriodToTheRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Commission> empty = new PageImpl<>(List.of());
        when(commissionRepository.search(eq(tenantId), any(), any(), any(OffsetDateTime.class), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(empty);

        commissionService.search(null, null, null, null, pageable);

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(commissionRepository).search(eq(tenantId), any(), any(), from.capture(), to.capture(), eq(pageable));
        assertThat(from.getValue()).isBefore(to.getValue());
    }

    @Test
    void report_neverPassesNullPeriodToTheRepository() {
        when(commissionRepository.reportByAgent(eq(tenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        assertThat(commissionService.report(null, null)).isEmpty();
    }
}
