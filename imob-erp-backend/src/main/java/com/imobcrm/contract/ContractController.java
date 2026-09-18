package com.imobcrm.contract;

import com.imobcrm.contract.dto.ContractRequestDTO;
import com.imobcrm.contract.dto.ContractResponseDTO;
import com.imobcrm.contract.dto.ContractStatusUpdateDTO;
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
    public Page<ContractResponseDTO> search(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType type,
            Pageable pageable) {
        return contractService.search(status, type, pageable);
    }

    @GetMapping("/{id}")
    public ContractResponseDTO findById(@PathVariable UUID id) {
        return contractService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ContractResponseDTO> create(@Valid @RequestBody ContractRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @PutMapping("/{id}")
    public ContractResponseDTO update(@PathVariable UUID id, @Valid @RequestBody ContractRequestDTO request) {
        return contractService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ContractResponseDTO updateStatus(@PathVariable UUID id, @Valid @RequestBody ContractStatusUpdateDTO request) {
        return contractService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/document")
    public ContractResponseDTO uploadDocument(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return contractService.uploadDocument(id, file);
    }
}
