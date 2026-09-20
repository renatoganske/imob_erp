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
import com.imobcrm.visit.api.VisitResponse;
import com.imobcrm.visit.domain.enums.VisitStatus;
import com.imobcrm.visit.infra.VisitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final OffsetDateTime PERIOD_MIN = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime PERIOD_MAX = OffsetDateTime.parse("9999-12-31T23:59:59Z");

    private final VisitRepository visitRepository;
    private final VisitMapper visitMapper;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    /**
     * Lista as visitas do tenant, opcionalmente filtradas por corretor, status e periodo (datas inclusivas,
     * no fuso de Brasilia). Corretor enxerga apenas as proprias visitas, ignorando qualquer agentId enviado.
     */
    @Transactional(readOnly = true)
    public Page<VisitResponse> search(UUID agentId, VisitStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        UUID effectiveAgentId = isCorretor() ? TenantContext.userId() : agentId;
        return visitRepository.search(TenantContext.tenantId(), effectiveAgentId, status, startOf(from), endOf(to), pageable)
                .map(visitMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public VisitResponse findById(UUID id) {
        return visitMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public VisitResponse create(VisitRequest request) {
        validateReferences(request);
        Visit visit = Visit.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .leadId(request.leadId())
                .propertyId(request.propertyId())
                .agentId(request.agentId())
                .scheduledAt(request.scheduledAt())
                .status(VisitStatus.AGENDADA)
                .build();
        Visit saved = visitRepository.save(visit);
        log.info("Visita agendada: id={} propertyId={} agentId={} scheduledAt={}",
                saved.getId(), saved.getPropertyId(), saved.getAgentId(), saved.getScheduledAt());
        return visitMapper.toResponseDTO(saved);
    }

    @Transactional
    public VisitResponse updateStatus(UUID id, VisitStatus status) {
        Visit visit = findOwned(id);
        visit.setStatus(status);
        log.info("Visita {} mudou de status para {}", id, status);
        return visitMapper.toResponseDTO(visitRepository.save(visit));
    }

    @Transactional
    public VisitResponse updateResult(UUID id, String result) {
        Visit visit = findOwned(id);
        visit.setResult(result);
        visit.setStatus(VisitStatus.REALIZADA);
        log.info("Visita {} registrada como realizada", id);
        return visitMapper.toResponseDTO(visitRepository.save(visit));
    }

    // Os ids vindos no body precisam pertencer ao tenant da requisicao; a FK do banco so garante existencia.
    private void validateReferences(VisitRequest request) {
        UUID tenantId = TenantContext.tenantId();
        requireNotSchedulingForOthers(request.agentId());
        if (!leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)) {
            throw new ResourceNotFoundException("Lead", request.leadId());
        }
        Property property = propertyRepository.findByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", request.propertyId()));
        if (property.getStatus() == PropertyStatus.VENDIDO) {
            throw new BusinessException("Nao e possivel agendar visita para imovel vendido", "PROPERTY_SOLD");
        }
        if (!userRepository.existsByIdAndTenantId(request.agentId(), tenantId)) {
            throw new ResourceNotFoundException("Corretor", request.agentId());
        }
    }

    /** Corretor so agenda visitas para si mesmo; agendar para outro corretor e prerrogativa do Admin. */
    private void requireNotSchedulingForOthers(UUID agentId) {
        if (isCorretor() && !agentId.equals(TenantContext.userId())) {
            throw new ForbiddenException("Corretor nao pode agendar visitas para outro corretor");
        }
    }

    /**
     * Busca a visita do tenant atual. Para CORRETOR, visita de outro corretor responde 404
     * (mesmo que se ela nao existisse), sem revelar a existencia do registro.
     */
    private Visit findOwned(UUID id) {
        return visitRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .filter(visit -> !isCorretor() || visit.getAgentId().equals(TenantContext.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("Visita", id));
    }

    private boolean isCorretor() {
        return TenantContext.role() == Role.CORRETOR;
    }

    // O PostgreSQL nao infere o tipo de um OffsetDateTime nulo em "(:from IS NULL OR ...)", entao o
    // periodo aberto e representado por limites extremos em vez de parametros nulos.
    private static OffsetDateTime startOf(LocalDate date) {
        return Optional.ofNullable(date)
                .map(d -> d.atStartOfDay(BRASILIA).toOffsetDateTime())
                .orElse(PERIOD_MIN);
    }

    /** Limite superior exclusivo: o dia final e inclusivo, entao vale o inicio do dia seguinte. */
    private static OffsetDateTime endOf(LocalDate date) {
        return Optional.ofNullable(date)
                .map(d -> d.plusDays(1).atStartOfDay(BRASILIA).toOffsetDateTime())
                .orElse(PERIOD_MAX);
    }
}
