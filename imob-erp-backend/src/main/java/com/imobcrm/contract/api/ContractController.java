package com.imobcrm.contract.api;

import com.imobcrm.contract.domain.ContractService;
import com.imobcrm.contract.domain.enums.ContractStatus;
import com.imobcrm.contract.domain.enums.ContractType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // Leitura tambem para CORRETOR, restrita no service aos contratos dele e sem dados sensiveis (IMOB-35).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO', 'CORRETOR')")
    public Page<ContractResponse> search(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType type,
            @RequestParam(required = false) Integer expiringInDays,
            Pageable pageable) {
        return contractService.search(status, type, expiringInDays, pageable);
    }

    // Alertas de vencimento (IMOB-28): dado financeiro agregado, sem escopo por corretor.
    @GetMapping("/expiring-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public ExpiringContractsSummary expiringSummary() {
        return contractService.expiringSummary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO', 'CORRETOR')")
    public ContractResponse findById(@PathVariable UUID id) {
        return contractService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public ContractResponse update(@PathVariable UUID id, @Valid @RequestBody ContractRequest request) {
        return contractService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public ContractResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody ContractStatusRequest request) {
        return contractService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public ContractResponse uploadDocument(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return contractService.uploadDocument(id, file);
    }
}
