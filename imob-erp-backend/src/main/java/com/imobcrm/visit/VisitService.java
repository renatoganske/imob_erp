package com.imobcrm.visit;

import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.visit.dto.VisitRequestDTO;
import com.imobcrm.visit.dto.VisitResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitMapper visitMapper;

    @Transactional(readOnly = true)
    public Page<VisitResponseDTO> search(UUID agentId, Pageable pageable) {
        return visitRepository.search(TenantContext.tenantId(), agentId, pageable)
                .map(visitMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public VisitResponseDTO findById(UUID id) {
        return visitMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public VisitResponseDTO create(VisitRequestDTO request) {
        Visit visit = Visit.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .leadId(request.leadId())
                .propertyId(request.propertyId())
                .agentId(request.agentId())
                .scheduledAt(request.scheduledAt())
                .status(VisitStatus.AGENDADA)
                .build();
        return visitMapper.toResponseDTO(visitRepository.save(visit));
    }

    @Transactional
    public VisitResponseDTO updateStatus(UUID id, VisitStatus status) {
        Visit visit = findOwned(id);
        visit.setStatus(status);
        return visitMapper.toResponseDTO(visitRepository.save(visit));
    }

    @Transactional
    public VisitResponseDTO updateResult(UUID id, String result) {
        Visit visit = findOwned(id);
        visit.setResult(result);
        visit.setStatus(VisitStatus.REALIZADA);
        return visitMapper.toResponseDTO(visitRepository.save(visit));
    }

    private Visit findOwned(UUID id) {
        return visitRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Visita", id));
    }
}
