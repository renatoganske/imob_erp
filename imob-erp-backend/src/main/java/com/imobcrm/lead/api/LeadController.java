package com.imobcrm.lead.api;

import com.imobcrm.lead.domain.LeadService;
import com.imobcrm.lead.domain.enums.LeadStage;
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
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public Page<LeadResponse> search(
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) LeadStage stage,
            Pageable pageable) {
        return leadService.search(assignedTo, stage, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public LeadResponse findById(@PathVariable UUID id) {
        return leadService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public LeadResponse update(@PathVariable UUID id, @Valid @RequestBody LeadRequest request) {
        return leadService.update(id, request);
    }

    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public LeadResponse updateStage(@PathVariable UUID id, @Valid @RequestBody LeadStageRequest request) {
        return leadService.updateStage(id, request.stage());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public LeadResponse assign(@PathVariable UUID id, @Valid @RequestBody LeadAssignRequest request) {
        return leadService.assign(id, request.agentId());
    }

    @PostMapping("/{id}/properties")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public LeadResponse addProperty(@PathVariable UUID id, @RequestBody UUID propertyId) {
        return leadService.addPropertyOfInterest(id, propertyId);
    }

    @DeleteMapping("/{id}/properties/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public LeadResponse removeProperty(@PathVariable UUID id, @PathVariable UUID propertyId) {
        return leadService.removePropertyOfInterest(id, propertyId);
    }
}
