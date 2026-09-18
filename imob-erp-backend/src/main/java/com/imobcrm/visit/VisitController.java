package com.imobcrm.visit;

import com.imobcrm.visit.dto.VisitRequestDTO;
import com.imobcrm.visit.dto.VisitResponseDTO;
import com.imobcrm.visit.dto.VisitResultUpdateDTO;
import com.imobcrm.visit.dto.VisitStatusUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public Page<VisitResponseDTO> search(@RequestParam(required = false) UUID agentId, Pageable pageable) {
        return visitService.search(agentId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public VisitResponseDTO findById(@PathVariable UUID id) {
        return visitService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public ResponseEntity<VisitResponseDTO> create(@Valid @RequestBody VisitRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public VisitResponseDTO updateStatus(@PathVariable UUID id, @Valid @RequestBody VisitStatusUpdateDTO request) {
        return visitService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public VisitResponseDTO updateResult(@PathVariable UUID id, @Valid @RequestBody VisitResultUpdateDTO request) {
        return visitService.updateResult(id, request.result());
    }
}
