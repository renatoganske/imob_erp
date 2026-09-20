package com.imobcrm.user.domain;

import com.imobcrm.config.AppCorsProperties;
import com.imobcrm.onboarding.ClerkUserDirectory;
import com.imobcrm.onboarding.ClerkUserDirectory.ClerkUser;
import com.imobcrm.shared.exception.ConflictException;
import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.api.UserInviteRequest;
import com.imobcrm.user.api.UserResponse;
import com.imobcrm.user.domain.enums.Role;
import com.imobcrm.user.infra.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ClerkUserDirectory clerk;
    private final AppCorsProperties corsProperties;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByTenantId(TenantContext.tenantId()).stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    /**
     * Convida por e-mail (IMOB-40). Conta nova: convite do Clerk com tenantId/role no metadata e registro
     * pendente, vinculado no primeiro acesso (InviteAcceptanceService). Conta que ja existe no Clerk:
     * vincula na hora. Falha do Clerk desfaz o registro local.
     */
    @Transactional
    public UserResponse invite(UserInviteRequest request) {
        UUID tenantId = TenantContext.tenantId();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Este e-mail ja esta vinculado a uma imobiliaria", "EMAIL_IN_USE");
        }
        Map<String, Object> metadata = Map.of("tenantId", tenantId.toString(), "role", request.role().name());

        Optional<ClerkUser> existing = clerk.findByVerifiedEmail(email);
        if (existing.isPresent()) {
            ClerkUser account = existing.get();
            if (account.tenantId() != null && !account.tenantId().equals(tenantId.toString())) {
                throw new ConflictException("Este e-mail ja pertence a outra imobiliaria", "EMAIL_IN_USE");
            }
            User linked = userRepository.save(newUser(tenantId, account.id(), request, email, true));
            clerk.mergePublicMetadata(account.id(), metadata);
            log.info("Usuario existente vinculado: id={} email={} role={}", linked.getId(), email, linked.getRole());
            return userMapper.toResponseDTO(linked);
        }

        User pending = userRepository.save(newUser(tenantId, "pending:" + UUID.randomUUID(), request, email, false));
        clerk.createInvitation(email, metadata, signUpUrl());
        log.info("Convite enviado: id={} email={} role={}", pending.getId(), email, pending.getRole());
        return userMapper.toResponseDTO(pending);
    }

    private User newUser(UUID tenantId, String clerkUserId, UserInviteRequest request, String email, boolean active) {
        return User.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .clerkUserId(clerkUserId)
                .name(request.name())
                .email(email)
                .role(request.role())
                .active(active)
                .build();
    }

    private String signUpUrl() {
        return corsProperties.allowedOrigins().split(",")[0].trim() + "/sign-up";
    }

    @Transactional
    public UserResponse updateRole(UUID id, Role role) {
        User user = findOwned(id);
        Role previousRole = user.getRole();
        user.setRole(role);
        log.info("Papel do usuario {} alterado: {} -> {}", id, previousRole, role);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateCommissionRate(UUID id, BigDecimal commissionRate) {
        User user = findOwned(id);
        user.setCommissionRate(commissionRate);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = findOwned(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("Usuario {} desativado", id);
    }

    private User findOwned(UUID id) {
        return userRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
