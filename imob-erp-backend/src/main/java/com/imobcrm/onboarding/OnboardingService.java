package com.imobcrm.onboarding;

import com.imobcrm.onboarding.ClerkUserDirectory.ClerkUser;
import com.imobcrm.shared.exception.BusinessException;
import com.imobcrm.shared.exception.ConflictException;
import com.imobcrm.tenant.Tenant;
import com.imobcrm.tenant.TenantRepository;
import com.imobcrm.user.domain.User;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Onboarding assistido (IMOB-38): o operador cria a imobiliaria e o administrador, e o backend grava
 * tenantId/role no publicMetadata do Clerk. Idempotente: repetir a chamada nao duplica nada e
 * re-sincroniza o metadata, entao uma falha do Clerk depois do commit se resolve repetindo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final ClerkUserDirectory clerk;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate tx;

    public TenantOnboardingResponse onboard(TenantOnboardingRequest request) {
        String email = request.adminEmail().trim().toLowerCase(Locale.ROOT);
        String slug = slugify(request.tenantName());
        ClerkUser clerkUser = clerk.findByVerifiedEmail(email).orElseThrow(() -> new BusinessException(
                "Nenhuma conta no Clerk com o e-mail " + email + " verificado. Peca ao cliente para se cadastrar antes.",
                "CLERK_USER_NOT_FOUND"));

        TenantOnboardingResponse result = tx.execute(status -> persist(request.tenantName().trim(), slug, email, clerkUser));

        // Fora da transacao: se o Clerk falhar, o banco ja esta correto e a repeticao so refaz esta chamada.
        clerk.mergePublicMetadata(clerkUser.id(), Map.of("tenantId", result.tenantId().toString(), "role", Role.ADMIN.name()));
        log.info("Onboarding: tenant={} slug={} admin={} created={}", result.tenantId(), slug, result.adminUserId(), result.created());
        return result;
    }

    private TenantOnboardingResponse persist(String tenantName, String slug, String email, ClerkUser clerkUser) {
        User existing = userRepository.findByClerkUserId(clerkUser.id()).orElse(null);
        if (existing != null) {
            Tenant tenant = tenantRepository.findById(existing.getTenantId()).orElseThrow();
            if (!tenant.getSlug().equals(slug)) {
                throw new ConflictException("Este e-mail ja pertence a imobiliaria '" + tenant.getName() + "'", "EMAIL_IN_USE");
            }
            existing.setRole(Role.ADMIN);
            existing.setActive(true);
            userRepository.save(existing);
            return response(tenant, existing, false);
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Este e-mail ja esta vinculado a uma imobiliaria (convite pendente ou usuario existente)", "EMAIL_IN_USE");
        }
        if (tenantRepository.findBySlug(slug).isPresent()) {
            throw new ConflictException("Ja existe uma imobiliaria com o identificador '" + slug + "'", "TENANT_SLUG_TAKEN");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name(tenantName)
                .slug(slug)
                .plan(Tenant.Plan.STARTER)
                .active(true)
                .build());
        User admin = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant.getId())
                .clerkUserId(clerkUser.id())
                .name(clerkUser.name())
                .email(email)
                .role(Role.ADMIN)
                .active(true)
                .build());
        return response(tenant, admin, true);
    }

    private static TenantOnboardingResponse response(Tenant tenant, User admin, boolean created) {
        return new TenantOnboardingResponse(tenant.getId(), tenant.getSlug(), admin.getId(), admin.getClerkUserId(), created);
    }

    static String slugify(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            throw new BusinessException("Nome da imobiliaria invalido: use letras ou numeros", "INVALID_TENANT_NAME");
        }
        return slug;
    }
}
