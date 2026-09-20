package com.imobcrm.visit.api;

import com.imobcrm.visit.domain.VisitService;
import com.imobcrm.visit.domain.enums.VisitStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public Page<VisitResponse> search(
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @SortDefault(sort = "scheduledAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return visitService.search(agentId, status, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public VisitResponse findById(@PathVariable UUID id) {
        return visitService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody VisitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public VisitResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody VisitStatusRequest request) {
        return visitService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public VisitResponse updateResult(@PathVariable UUID id, @Valid @RequestBody VisitResultRequest request) {
        return visitService.updateResult(id, request.result());
    }
}
