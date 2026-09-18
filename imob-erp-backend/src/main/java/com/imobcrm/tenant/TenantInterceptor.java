package com.imobcrm.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Garante que toda requisicao autenticada tenha um TenantContext valido
 * antes de alcancar qualquer Controller. A extracao do principal a partir
 * do JWT ja ocorre no ClerkJwtAuthenticationFilter; aqui apenas validamos
 * que o contexto foi de fato populado (RN-01).
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
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
