package com.imobcrm.property.domain;

import com.imobcrm.property.api.PropertyRequest;
import com.imobcrm.property.api.PropertyResponse;
import com.imobcrm.property.domain.enums.PropertyStatus;
import com.imobcrm.property.domain.enums.PropertyType;
import com.imobcrm.property.infra.PropertyMapper;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.storage.FileKind;
import com.imobcrm.storage.R2StorageService;
import com.imobcrm.storage.UploadValidator;
import com.imobcrm.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final int MAX_PHOTOS = 20;
    // RN-11: no maximo 20 fotos por imovel; apenas JPG/PNG/WebP (magic bytes) de ate 10MB, via UploadValidator

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final R2StorageService storageService;
    private final UploadValidator uploadValidator;

    @Transactional(readOnly = true)
    public Page<PropertyResponse> search(PropertyType type, PropertyStatus status, String neighborhood,
                                             BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return propertyRepository
                .search(TenantContext.tenantId(), type, status, neighborhood, minPrice, maxPrice, pageable)
                .map(propertyMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PropertyResponse findById(UUID id) {
        return propertyMapper.toResponseDTO(findOwned(id));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request) {
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
    public PropertyResponse update(UUID id, PropertyRequest request) {
        Property property = findOwned(id);
        propertyMapper.updateFromRequest(request, property);
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse updateStatus(UUID id, PropertyStatus status) {
        Property property = findOwned(id);
        // RN-04: imovel com contrato ativo so muda de status via fluxo de contrato.
        if (property.getStatus() == PropertyStatus.RESERVADO || property.getStatus() == PropertyStatus.VENDIDO
                || property.getStatus() == PropertyStatus.ALUGADO) {
            throw new BusinessException(
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

    /**
     * A linha do imovel fica travada durante toda a operacao: o Hibernate regrava a lista de fotos por inteiro,
     * entao uploads simultaneos (o front envia varias fotos em paralelo) se sobrescreveriam e duplicariam fotos.
     */
    @Transactional
    public PropertyResponse addPhoto(UUID id, MultipartFile file) {
        Property property = findOwnedForUpdate(id);
        FileKind kind = uploadValidator.validate(file, UploadValidator.IMAGES);
        requireRoomForAnotherPhoto(property);
        property.getPhotos().add(storageService.upload(photoKeyPrefix(property), file, kind));
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse removePhoto(UUID id, String photoKey) {
        Property property = findOwnedForUpdate(id);
        // photoKey e apenas o nome do arquivo (ultimo segmento); a chave completa no R2 e reconstruida
        // com o mesmo prefixo do upload em vez de confiar em uma chave completa vinda do cliente.
        String photoUrl = property.getPhotos().stream()
                .filter(endsWithSegment(photoKey))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Foto", photoKey));
        property.getPhotos().remove(photoUrl);
        storageService.delete(photoKeyPrefix(property) + "/" + photoKey);
        return propertyMapper.toResponseDTO(propertyRepository.save(property));
    }

    private static void requireRoomForAnotherPhoto(Property property) {
        if (property.getPhotos().size() >= MAX_PHOTOS) {
            throw new BusinessException("Maximo de " + MAX_PHOTOS + " fotos por imovel", "MAX_PHOTOS_EXCEEDED");
        }
    }

    private static Predicate<String> endsWithSegment(String segment) {
        return url -> url.endsWith("/" + segment);
    }

    private static String photoKeyPrefix(Property property) {
        return TenantContext.tenantId() + "/properties/" + property.getId();
    }

    private Property findOwned(UUID id) {
        return propertyRepository.findByIdAndTenantIdAndActiveTrue(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", id));
    }

    private Property findOwnedForUpdate(UUID id) {
        return propertyRepository.lockByIdAndTenantIdAndActiveTrue(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Imovel", id));
    }
}
