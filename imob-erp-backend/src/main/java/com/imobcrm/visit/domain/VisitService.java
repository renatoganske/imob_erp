package com.imobcrm.visit.domain;

import com.imobcrm.lead.domain.LeadRepository;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.UserRepository;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitMapper visitMapper;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<VisitResponse> search(UUID agentId, Pageable pageable) {
        return visitRepository.search(TenantContext.tenantId(), agentId, pageable)
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
        if (!leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)) {
            throw new ResourceNotFoundException("Lead", request.leadId());
        }
        if (!propertyRepository.existsByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)) {
            throw new ResourceNotFoundException("Imovel", request.propertyId());
        }
        if (!userRepository.existsByIdAndTenantId(request.agentId(), tenantId)) {
            throw new ResourceNotFoundException("Corretor", request.agentId());
        }
    }

    private Visit findOwned(UUID id) {
        return visitRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Visita", id));
    }
}
