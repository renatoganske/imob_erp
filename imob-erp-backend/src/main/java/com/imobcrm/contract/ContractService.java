package com.imobcrm.contract;

import com.imobcrm.commission.CommissionService;
import com.imobcrm.contract.dto.ContractRequestDTO;
import com.imobcrm.contract.dto.ContractResponseDTO;
import com.imobcrm.financial.FinancialCategory;
import com.imobcrm.financial.FinancialService;
import com.imobcrm.financial.FinancialType;
import com.imobcrm.property.Property;
import com.imobcrm.property.PropertyRepository;
import com.imobcrm.property.PropertyStatus;
import com.imobcrm.shared.exception.BusinessRuleException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.User;
import com.imobcrm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final FinancialService financialService;
    private final CommissionService commissionService;
    private final R2StorageService storageService;

    @Transactional(readOnly = true)
    public Page<ContractResponseDTO> search(ContractStatus status, ContractType type, Pageable pageable) {
        return contractRepository.search(TenantContext.tenantId(), status, type, pageable)
                .map(contractMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ContractResponseDTO findById(UUID id) {
        return contractMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public ContractResponseDTO create(ContractRequestDTO request) {
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
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    @Transactional
    public ContractResponseDTO update(UUID id, ContractRequestDTO request) {
        Contract contract = findOwned(id);
        if (contract.getStatus() != ContractStatus.RASCUNHO) {
            throw new BusinessRuleException("Somente contratos em rascunho podem ser editados", "CONTRACT_NOT_EDITABLE");
        }
        contractMapper.updateFromRequest(request, contract);
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    @Transactional
    public ContractResponseDTO updateStatus(UUID id, ContractStatus newStatus) {
        Contract contract = findOwned(id);
        switch (newStatus) {
            case ATIVO -> activate(contract);
            case ENCERRADO -> contract.setStatus(ContractStatus.ENCERRADO);
            case CANCELADO -> contract.setStatus(ContractStatus.CANCELADO);
            case RASCUNHO -> throw new BusinessRuleException("Nao e possivel voltar um contrato para rascunho", "INVALID_STATUS_TRANSITION");
        }
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    /**
     * RN-03: ao ativar, atualiza o status do imovel e gera parcelas financeiras
     * e comissao do corretor. Acao irreversivel sem cancelamento explicito.
     */
    private void activate(Contract contract) {
        if (contract.getStatus() != ContractStatus.RASCUNHO) {
            throw new BusinessRuleException("Somente contratos em rascunho podem ser ativados", "CONTRACT_NOT_DRAFT");
        }
        contract.setStatus(ContractStatus.ATIVO);

        Property property = propertyRepository.findByIdAndTenantIdAndActiveTrue(contract.getPropertyId(), TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", contract.getPropertyId()));
        property.setStatus(contract.getType() == ContractType.LOCACAO ? PropertyStatus.ALUGADO : PropertyStatus.VENDIDO);
        propertyRepository.save(property);

        if (contract.getType() == ContractType.LOCACAO) {
            financialService.generateRentInstallments(contract.getId(), contract.getValue(), contract.getStartDate());
        } else {
            financialService.createSingleEntry(contract.getId(), FinancialType.RECEITA, FinancialCategory.PARCELA_VENDA,
                    "Venda - " + contract.getId(), contract.getValue(), contract.getStartDate());
        }

        User agent = userRepository.findByIdAndTenantId(contract.getAgentId(), TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Corretor", contract.getAgentId()));
        BigDecimal rate = contract.getCommissionRateOverride() != null
                ? contract.getCommissionRateOverride()
                : agent.getCommissionRate();
        commissionService.createFromContract(contract.getId(), agent.getId(), contract.getValue(), rate);
    }

    @Transactional
    public ContractResponseDTO uploadDocument(UUID id, MultipartFile file) {
        Contract contract = findOwned(id);
        String keyPrefix = TenantContext.tenantId() + "/contracts/" + contract.getId();
        String url = storageService.upload(keyPrefix, file);
        contract.setDocumentUrl(url);
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    private Contract findOwned(UUID id) {
        return contractRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", id));
    }
}
