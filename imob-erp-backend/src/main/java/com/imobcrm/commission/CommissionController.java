package com.imobcrm.commission;

import com.imobcrm.commission.dto.CommissionReportItemDTO;
import com.imobcrm.commission.dto.CommissionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR', 'FINANCEIRO')")
    public Page<CommissionResponseDTO> search(
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {
        return commissionService.search(agentId, status, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR', 'FINANCEIRO')")
    public CommissionResponseDTO findById(@PathVariable UUID id) {
        return commissionService.findById(id);
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public CommissionResponseDTO pay(@PathVariable UUID id) {
        return commissionService.pay(id);
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    public List<CommissionReportItemDTO> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return commissionService.report(from, to);
    }
}
