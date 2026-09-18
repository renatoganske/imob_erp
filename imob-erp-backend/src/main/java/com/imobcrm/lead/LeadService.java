package com.imobcrm.lead;

import com.imobcrm.contract.ContractService;
import com.imobcrm.contract.ContractType;
import com.imobcrm.contract.dto.ContractRequestDTO;
import com.imobcrm.lead.dto.LeadRequestDTO;
import com.imobcrm.lead.dto.LeadResponseDTO;
import com.imobcrm.property.Property;
import com.imobcrm.property.PropertyPurpose;
import com.imobcrm.property.PropertyRepository;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;
    private final ContractService contractService;

    @Transactional(readOnly = true)
    public Page<LeadResponseDTO> search(UUID assignedTo, LeadStage stage, Pageable pageable) {
        return leadRepository.search(TenantContext.tenantId(), assignedTo, stage, pageable)
                .map(leadMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public LeadResponseDTO findById(UUID id) {
        return leadMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public LeadResponseDTO create(LeadRequestDTO request) {
        UUID assignedTo = request.assignedTo() != null ? request.assignedTo() : TenantContext.userId();
        validateAgentExists(assignedTo);
        Lead lead = Lead.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .assignedTo(assignedTo)
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .source(request.source())
                .stage(LeadStage.NOVO)
                .notes(request.notes())
                .build();
        Lead saved = leadRepository.save(lead);
        log.info("Lead criado: id={} assignedTo={} source={}", saved.getId(), saved.getAssignedTo(), saved.getSource());
        return leadMapper.toResponseDTO(saved);
    }

    @Transactional
    public LeadResponseDTO update(UUID id, LeadRequestDTO request) {
        Lead lead = findOwned(id);
        lead.setName(request.name());
        lead.setPhone(request.phone());
        lead.setEmail(request.email());
        lead.setSource(request.source());
        lead.setNotes(request.notes());
        if (request.assignedTo() != null) {
            validateAgentExists(request.assignedTo());
            lead.setAssignedTo(request.assignedTo());
        }
        return leadMapper.toResponseDTO(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponseDTO updateStage(UUID id, LeadStage stage) {
        Lead lead = findOwned(id);
        LeadStage previousStage = lead.getStage();
        boolean movingToFechado = stage == LeadStage.FECHADO && previousStage != LeadStage.FECHADO;
        lead.setStage(stage);
        Lead saved = leadRepository.save(lead);
        log.info("Lead {} mudou de estagio: {} -> {}", saved.getId(), previousStage, stage);

        // RN-02: ao mover lead para FECHADO, cria automaticamente um rascunho
        // de contrato. O Admin/Financeiro revisa os dados pendentes e ativa manualmente.
        if (movingToFechado) {
            createDraftContractFromLead(saved);
        }

        return leadMapper.toResponseDTO(saved);
    }

    private void createDraftContractFromLead(Lead lead) {
        if (lead.getPropertiesOfInterest().isEmpty()) {
            log.warn("Lead {} fechado sem imoveis de interesse; rascunho de contrato nao foi gerado", lead.getId());
            return;
        }
        Property property = lead.getPropertiesOfInterest().iterator().next();
        ContractType type = property.getPurpose() == PropertyPurpose.ALUGUEL
                ? ContractType.LOCACAO
                : ContractType.COMPRA_VENDA;

        ContractRequestDTO draft = new ContractRequestDTO(
                lead.getId(),
                property.getId(),
                lead.getAssignedTo(),
                type,
                property.getPrice(),
                null,
                LocalDate.now(),
                null,
                null,
                lead.getName(),
                "PENDENTE",
                "PENDENTE",
                "PENDENTE",
                null,
                "Rascunho gerado automaticamente a partir do lead " + lead.getId());
        var contract = contractService.create(draft);
        log.info("Rascunho de contrato {} gerado automaticamente a partir do lead {}", contract.id(), lead.getId());
    }

    @Transactional
    public LeadResponseDTO assign(UUID id, UUID agentId) {
        validateAgentExists(agentId);
        Lead lead = findOwned(id);
        lead.setAssignedTo(agentId);
        log.info("Lead {} reatribuido ao corretor {}", id, agentId);
        return leadMapper.toResponseDTO(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponseDTO addPropertyOfInterest(UUID id, UUID propertyId) {
        Lead lead = findOwned(id);
        Property property = propertyRepository.findByIdAndTenantIdAndActiveTrue(propertyId, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", propertyId));
        lead.getPropertiesOfInterest().add(property);
        return leadMapper.toResponseDTO(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponseDTO removePropertyOfInterest(UUID id, UUID propertyId) {
        Lead lead = findOwned(id);
        lead.getPropertiesOfInterest().removeIf(p -> p.getId().equals(propertyId));
        return leadMapper.toResponseDTO(leadRepository.save(lead));
    }

    private Lead findOwned(UUID id) {
        return leadRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead", id));
    }

    private void validateAgentExists(UUID agentId) {
        if (!userRepository.findByIdAndTenantId(agentId, TenantContext.tenantId()).isPresent()) {
            throw new ResourceNotFoundException("Corretor", agentId);
        }
    }
}
