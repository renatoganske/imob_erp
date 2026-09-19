package com.imobcrm.contract.domain;

import com.imobcrm.contract.domain.enums.AdjustmentIndex;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import com.imobcrm.shared.audit.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contracts")
public class Contract extends AuditEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal value;

    @Column(name = "signed_at")
    private LocalDate signedAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_index")
    private AdjustmentIndex adjustmentIndex;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "buyer_document", nullable = false)
    private String buyerDocument;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "owner_document", nullable = false)
    private String ownerDocument;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "commission_rate_override", precision = 5, scale = 2)
    private BigDecimal commissionRateOverride;

    @Column(columnDefinition = "text")
    private String notes;
}
