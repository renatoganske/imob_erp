package com.imobcrm.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Garante que toda requisicao autenticada tenha um TenantContext valido
 * antes de alcancar qualquer Controller. A extracao do principal a partir
 * do JWT ja ocorre no ClerkJwtAuthenticationFilter; aqui apenas validamos
 * que o contexto foi de fato populado (RN-01) e enriquecemos o MDC com
 * tenantId/userId para correlacionar logs da requisicao.
 *
 * A limpeza de TenantContext e MDC fica a cargo do RequestLoggingFilter,
 * que roda para toda requisicao (inclusive as que nunca alcancam um
 * Controller, como um 403 de autorizacao).
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true; // deixa o Spring Security responder 401
        }
        if (!(authentication.getPrincipal() instanceof TenantContext.RequestPrincipal principal)) {
            return true;
        }
        TenantContext.set(principal);
        MDC.put("tenantId", principal.tenantId().toString());
        MDC.put("userId", principal.userId().toString());
        return true;
    }
}
