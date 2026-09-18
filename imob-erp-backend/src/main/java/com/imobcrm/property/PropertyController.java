package com.imobcrm.property;

import com.imobcrm.property.dto.PropertyRequestDTO;
import com.imobcrm.property.dto.PropertyResponseDTO;
import com.imobcrm.property.dto.PropertyStatusUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public Page<PropertyResponseDTO> search(
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return propertyService.search(type, status, neighborhood, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public PropertyResponseDTO findById(@PathVariable UUID id) {
        return propertyService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public ResponseEntity<PropertyResponseDTO> create(@Valid @RequestBody PropertyRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public PropertyResponseDTO update(@PathVariable UUID id, @Valid @RequestBody PropertyRequestDTO request) {
        return propertyService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public PropertyResponseDTO updateStatus(@PathVariable UUID id, @Valid @RequestBody PropertyStatusUpdateDTO request) {
        return propertyService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public PropertyResponseDTO addPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return propertyService.addPhoto(id, file);
    }

    @DeleteMapping("/{id}/photos/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORRETOR')")
    public PropertyResponseDTO removePhoto(@PathVariable UUID id, @PathVariable String key) {
        return propertyService.removePhoto(id, key);
    }
}
