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
import com.imobcrm.storage.FileKind;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.storage.UploadValidator;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.User;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;
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
    private final UploadValidator uploadValidator;

    @Transactional(readOnly = true)
    public Page<ContractResponse> search(ContractStatus status, ContractType type, Pageable pageable) {
        // Corretor enxerga apenas os contratos em que e o corretor responsavel.
        UUID agentScope = isCorretor() ? TenantContext.userId() : null;
        return contractRepository.search(TenantContext.tenantId(), agentScope, status, type, pageable)
                .map(this::toReadResponse);
    }

    @Transactional(readOnly = true)
    public ContractResponse findById(UUID id) {
        Contract contract = findOwned(id);
        if (isCorretor() && !contract.getAgentId().equals(TenantContext.userId())) {
            // 404, igual a um contrato inexistente, para nao revelar a existencia do registro
            throw new ResourceNotFoundException("Contrato", id);
        }
        return toReadResponse(contract);
    }

    private ContractResponse toReadResponse(Contract contract) {
        ContractResponse response = contractMapper.toResponseDTO(contract);
        return isCorretor() ? response.withoutSensitiveData() : response;
    }

    private boolean isCorretor() {
        return TenantContext.role() == Role.CORRETOR;
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
        FileKind kind = uploadValidator.validate(file, Set.of(FileKind.PDF));
        String keyPrefix = TenantContext.tenantId() + "/contracts/" + contract.getId();
        String previousUrl = contract.getDocumentUrl();

        String url = storageService.upload(keyPrefix, file, kind);
        contract.setDocumentUrl(url);
        Contract saved = contractRepository.save(contract);

        // Um PDF por contrato: o anterior e apagado do R2 apenas depois do commit (se o banco falhar, o
        // contrato continua apontando para um arquivo que existe) e o novo e apagado se a transacao for revertida.
        afterCompletion(committed -> {
            if (committed) {
                deletePreviousDocument(contract, previousUrl);
            } else {
                storageService.keyFromUrl(url).ifPresent(storageService::delete);
            }
        });
        return contractMapper.toResponseDTO(saved);
    }

    private void deletePreviousDocument(Contract contract, String previousUrl) {
        String expectedPrefix = TenantContext.tenantId() + "/contracts/" + contract.getId() + "/";
        storageService.keyFromUrl(previousUrl)
                .filter(key -> key.startsWith(expectedPrefix))
                .ifPresent(key -> {
                    try {
                        storageService.delete(key);
                    } catch (RuntimeException e) {
                        log.warn("Nao foi possivel apagar o documento anterior do contrato {} (key={}): {}",
                                contract.getId(), key, e.getMessage());
                    }
                });
    }

    private static void afterCompletion(java.util.function.Consumer<Boolean> action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.accept(true);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                action.accept(status == STATUS_COMMITTED);
            }
        });
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
