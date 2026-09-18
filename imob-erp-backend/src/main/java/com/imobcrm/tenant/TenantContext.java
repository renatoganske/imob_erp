package com.imobcrm.tenant;

import com.imobcrm.user.Role;

import java.util.UUID;

/**
 * Contexto da requisicao atual, populado pelo ClerkJwtAuthenticationFilter
 * a partir do publicMetadata do JWT (tenantId, userId, role).
 */
public final class TenantContext {

    private static final ThreadLocal<RequestPrincipal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(RequestPrincipal principal) {
        CURRENT.set(principal);
    }

    public static RequestPrincipal get() {
        RequestPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new IllegalStateException("TenantContext nao foi inicializado para esta requisicao");
        }
        return principal;
    }

    public static UUID tenantId() {
        return get().tenantId();
    }

    public static UUID userId() {
        return get().userId();
    }

    public static Role role() {
        return get().role();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record RequestPrincipal(UUID tenantId, UUID userId, String clerkUserId, Role role) {
    }
}
