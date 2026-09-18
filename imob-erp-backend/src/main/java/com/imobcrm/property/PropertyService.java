package com.imobcrm.property;

import com.imobcrm.shared.exception.BusinessRuleException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.property.dto.PropertyRequestDTO;
import com.imobcrm.property.dto.PropertyResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final int MAX_PHOTOS = 20;
    // RN-11: apenas JPG/PNG/WebP, ate 10MB (limite de tamanho aplicado via spring.servlet.multipart.max-file-size)
    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final R2StorageService storageService;

    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> search(PropertyType type, PropertyStatus status, String neighborhood,
                                             BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return propertyRepository
                .search(TenantContext.tenantId(), type, status, neighborhood, minPrice, maxPrice, pageable)
                .map(propertyMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PropertyResponseDTO findById(UUID id) {
        return propertyMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public PropertyResponseDTO create(PropertyRequestDTO request) {
        Property property = Property.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .address(request.address())
                .neighborhood(request.neighborhood())
                .city(request.city())
                .price(request.price())
                .area(request.area())
                .bedrooms(request.bedrooms())
                .bathrooms(request.bathrooms())
                .parkingSpots(request.parkingSpots())
                .purpose(request.purpose())
                .status(PropertyStatus.DISPONIVEL)
                .build();
        Property saved = propertyRepository.save(property);
        log.info("Imovel criado: id={} title={} price={}", saved.getId(), saved.getTitle(), saved.getPrice());
        return propertyMapper.toResponseDTO(saved);
    }

    @Transactional
    public PropertyResponseDTO update(UUID id, PropertyRequestDTO request) {
        Property property = findOwned(id);
        propertyMapper.updateFromRequest(request, property);
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponseDTO updateStatus(UUID id, PropertyStatus status) {
        Property property = findOwned(id);
        // RN-04: imovel com contrato ativo so muda de status via fluxo de contrato.
        if (property.getStatus() == PropertyStatus.RESERVADO || property.getStatus() == PropertyStatus.VENDIDO
                || property.getStatus() == PropertyStatus.ALUGADO) {
            throw new BusinessRuleException(
                    "Imovel com contrato ativo so pode ter o status alterado pelo fluxo de contrato",
                    "PROPERTY_STATUS_LOCKED");
        }
        PropertyStatus previousStatus = property.getStatus();
        property.setStatus(status);
        log.info("Imovel {} mudou de status: {} -> {}", id, previousStatus, status);
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    @Transactional
    public void delete(UUID id) {
        // RN-10: soft delete — imovel nunca e apagado do banco.
        Property property = findOwned(id);
        property.setActive(false);
        propertyRepository.save(property);
        log.info("Imovel {} desativado (soft delete)", id);
    }

    @Transactional
    public PropertyResponseDTO addPhoto(UUID id, MultipartFile file) {
        Property property = findOwned(id);
        if (property.getPhotos().size() >= MAX_PHOTOS) {
            throw new BusinessRuleException("Maximo de " + MAX_PHOTOS + " fotos por imovel", "MAX_PHOTOS_EXCEEDED");
        }
        if (!ALLOWED_PHOTO_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("Formato de imagem invalido. Use JPG, PNG ou WebP.", "INVALID_PHOTO_FORMAT");
        }
        String key = TenantContext.tenantId() + "/properties/" + property.getId();
        String url = storageService.upload(key, file);
        property.getPhotos().add(url);
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponseDTO removePhoto(UUID id, String photoKey) {
        Property property = findOwned(id);
        // photoKey e apenas o nome do arquivo (ultimo segmento); a chave completa no R2
        // segue o mesmo prefixo usado no upload, entao e reconstruida aqui em vez de
        // confiar em um valor de chave completo vindo do cliente.
        String fullKey = TenantContext.tenantId() + "/properties/" + property.getId() + "/" + photoKey;
        boolean removed = property.getPhotos().removeIf(url -> url.endsWith("/" + photoKey));
        if (!removed) {
            throw new ResourceNotFoundException("Foto", photoKey);
        }
        storageService.delete(fullKey);
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    private Property findOwned(UUID id) {
        return propertyRepository.findByIdAndTenantIdAndActiveTrue(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", id));
    }
}
