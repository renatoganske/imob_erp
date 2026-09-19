package com.imobcrm.financial.api;

import com.imobcrm.financial.domain.FinancialService;
import com.imobcrm.financial.domain.enums.EntryCategory;
import com.imobcrm.financial.domain.enums.EntryStatus;
import com.imobcrm.financial.domain.enums.EntryType;
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
    public Page<FinancialEntryResponse> search(
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) EntryStatus status,
            @RequestParam(required = false) EntryCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return financialService.search(type, status, category, from, to, pageable);
    }

    @GetMapping("/entries/{id}")
    public FinancialEntryResponse findById(@PathVariable UUID id) {
        return financialService.findById(id);
    }

    @PostMapping("/entries")
    public ResponseEntity<FinancialEntryResponse> create(@Valid @RequestBody FinancialEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialService.create(request));
    }

    @PatchMapping("/entries/{id}/pay")
    public FinancialEntryResponse pay(@PathVariable UUID id) {
        return financialService.pay(id);
    }

    @PatchMapping("/entries/{id}/cancel")
    public FinancialEntryResponse cancel(@PathVariable UUID id) {
        return financialService.cancel(id);
    }

    @GetMapping("/dashboard")
    public FinancialDashboardResponse dashboard() {
        return financialService.dashboard();
    }
}
