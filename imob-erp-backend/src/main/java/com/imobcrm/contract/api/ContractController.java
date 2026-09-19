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
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public Page<ContractResponse> search(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType type,
            Pageable pageable) {
        return contractService.search(status, type, pageable);
    }

    @GetMapping("/{id}")
    public ContractResponse findById(@PathVariable UUID id) {
        return contractService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @PutMapping("/{id}")
    public ContractResponse update(@PathVariable UUID id, @Valid @RequestBody ContractRequest request) {
        return contractService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ContractResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody ContractStatusRequest request) {
        return contractService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/document")
    public ContractResponse uploadDocument(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return contractService.uploadDocument(id, file);
    }
}
