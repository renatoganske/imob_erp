package com.imobcrm.contract.domain;

import com.imobcrm.commission.domain.CommissionService;
import com.imobcrm.contract.api.ContractRequest;
import com.imobcrm.contract.api.ContractResponse;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import com.imobcrm.contract.infra.ContractMapper;
import com.imobcrm.financial.domain.FinancialService;
import com.imobcrm.lead.domain.LeadRepository;
import com.imobcrm.financial.domain.enums.EntryCategory;
import com.imobcrm.financial.domain.enums.EntryType;
import com.imobcrm.property.domain.Property;
import com.imobcrm.property.domain.PropertyRepository;
import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.User;
import com.imobcrm.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final FinancialService financialService;
    private final CommissionService commissionService;
    private final R2StorageService storageService;

    @Transactional(readOnly = true)
    public Page<ContractResponse> search(ContractStatus status, ContractType type, Pageable pageable) {
        return contractRepository.search(TenantContext.tenantId(), status, type, pageable)
                .map(contractMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ContractResponse findById(UUID id) {
        return contractMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public ContractResponse create(ContractRequest request) {
        validateReferences(request);
        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .leadId(request.leadId())
                .propertyId(request.propertyId())
                .agentId(request.agentId())
                .type(request.type())
                .status(ContractStatus.RASCUNHO)
                .value(request.value())
                .signedAt(request.signedAt())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .adjustmentIndex(request.adjustmentIndex())
                .buyerName(request.buyerName())
                .buyerDocument(request.buyerDocument())
                .ownerName(request.ownerName())
                .ownerDocument(request.ownerDocument())
                .commissionRateOverride(request.commissionRateOverride())
                .notes(request.notes())
                .build();
        Contract saved = contractRepository.save(contract);
        log.info("Contrato criado: id={} type={} value={} propertyId={}", saved.getId(), saved.getType(), saved.getValue(), saved.getPropertyId());
        return contractMapper.toResponseDTO(saved);
    }

    @Transactional
    public ContractResponse update(UUID id, ContractRequest request) {
        Contract contract = findOwned(id);
        if (contract.getStatus() != ContractStatus.RASCUNHO) {
            throw new BusinessException("Somente contratos em rascunho podem ser editados", "CONTRACT_NOT_EDITABLE");
        }
        validateReferences(request);
        contractMapper.updateFromRequest(request, contract);
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    @Transactional
    public ContractResponse updateStatus(UUID id, ContractStatus newStatus) {
        Contract contract = findOwned(id);
        switch (newStatus) {
            case ATIVO -> activate(contract);
            case ENCERRADO -> contract.setStatus(ContractStatus.ENCERRADO);
            case CANCELADO -> contract.setStatus(ContractStatus.CANCELADO);
            case RASCUNHO -> throw new BusinessException("Nao e possivel voltar um contrato para rascunho", "INVALID_STATUS_TRANSITION");
        }
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    /**
     * RN-03: ao ativar, atualiza o status do imovel e gera parcelas financeiras
     * e comissao do corretor. Acao irreversivel sem cancelamento explicito.
     */
    private void activate(Contract contract) {
        if (contract.getStatus() != ContractStatus.RASCUNHO) {
            throw new BusinessException("Somente contratos em rascunho podem ser ativados", "CONTRACT_NOT_DRAFT");
        }

        // Valida corretor e taxa antes de qualquer efeito: contrato sem comissao definida nao pode ser ativado.
        User agent = userRepository.findByIdAndTenantId(contract.getAgentId(), TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Corretor", contract.getAgentId()));
        BigDecimal rate = contract.getCommissionRateOverride() != null
                ? contract.getCommissionRateOverride()
                : agent.getCommissionRate();
        if (rate == null) {
            throw new BusinessException(
                    "O corretor " + agent.getName() + " nao tem taxa de comissao definida. Defina a taxa em Usuarios antes de ativar o contrato",
                    "AGENT_WITHOUT_COMMISSION_RATE");
        }

        contract.setStatus(ContractStatus.ATIVO);
        log.info("Ativando contrato {}: gerando parcelas financeiras e comissao do corretor", contract.getId());

        Property property = propertyRepository.findByIdAndTenantIdAndActiveTrue(contract.getPropertyId(), TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", contract.getPropertyId()));
        property.setStatus(contract.getType() == ContractType.LOCACAO ? PropertyStatus.ALUGADO : PropertyStatus.VENDIDO);
        propertyRepository.save(property);

        if (contract.getType() == ContractType.LOCACAO) {
            financialService.generateRentInstallments(contract.getId(), contract.getValue(), contract.getStartDate());
        } else {
            financialService.createSingleEntry(contract.getId(), EntryType.RECEITA, EntryCategory.PARCELA_VENDA,
                    "Venda - " + contract.getId(), contract.getValue(), contract.getStartDate());
        }

        commissionService.createFromContract(contract.getId(), agent.getId(), contract.getValue(), rate);
    }

    @Transactional
    public ContractResponse uploadDocument(UUID id, MultipartFile file) {
        Contract contract = findOwned(id);
        String keyPrefix = TenantContext.tenantId() + "/contracts/" + contract.getId();
        String url = storageService.upload(keyPrefix, file);
        contract.setDocumentUrl(url);
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    // Os ids vindos no body precisam pertencer ao tenant da requisicao; a FK do banco so garante existencia.
    private void validateReferences(ContractRequest request) {
        UUID tenantId = TenantContext.tenantId();
        if (request.leadId() != null && !leadRepository.existsByIdAndTenantId(request.leadId(), tenantId)) {
            throw new ResourceNotFoundException("Lead", request.leadId());
        }
        if (!propertyRepository.existsByIdAndTenantIdAndActiveTrue(request.propertyId(), tenantId)) {
            throw new ResourceNotFoundException("Imovel", request.propertyId());
        }
        if (!userRepository.existsByIdAndTenantId(request.agentId(), tenantId)) {
            throw new ResourceNotFoundException("Corretor", request.agentId());
        }
    }

    private Contract findOwned(UUID id) {
        return contractRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", id));
    }
}
