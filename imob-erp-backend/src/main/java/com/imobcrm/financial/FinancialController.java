package com.imobcrm.financial;

import com.imobcrm.financial.dto.FinancialDashboardDTO;
import com.imobcrm.financial.dto.FinancialEntryRequestDTO;
import com.imobcrm.financial.dto.FinancialEntryResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financial")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
public class FinancialController {

    private final FinancialService financialService;

    @GetMapping("/entries")
    public Page<FinancialEntryResponseDTO> search(
            @RequestParam(required = false) FinancialType type,
            @RequestParam(required = false) FinancialStatus status,
            @RequestParam(required = false) FinancialCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return financialService.search(type, status, category, from, to, pageable);
    }

    @GetMapping("/entries/{id}")
    public FinancialEntryResponseDTO findById(@PathVariable UUID id) {
        return financialService.findById(id);
    }

    @PostMapping("/entries")
    public ResponseEntity<FinancialEntryResponseDTO> create(@Valid @RequestBody FinancialEntryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialService.create(request));
    }

    @PatchMapping("/entries/{id}/pay")
    public FinancialEntryResponseDTO pay(@PathVariable UUID id) {
        return financialService.pay(id);
    }

    @PatchMapping("/entries/{id}/cancel")
    public FinancialEntryResponseDTO cancel(@PathVariable UUID id) {
        return financialService.cancel(id);
    }

    @GetMapping("/dashboard")
    public FinancialDashboardDTO dashboard() {
        return financialService.dashboard();
    }
}
