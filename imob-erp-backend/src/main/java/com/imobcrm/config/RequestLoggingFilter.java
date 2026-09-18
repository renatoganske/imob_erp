package com.imobcrm.config;

import com.imobcrm.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Garante uma linha de log de acesso (metodo, path, status, duracao) para
 * toda requisicao, mesmo quando ela e barrada antes de alcancar um
 * Controller (401/403). Um HandlerInterceptor nao e suficiente aqui: seu
 * afterCompletion nao roda quando o acesso e negado via @PreAuthorize, o
 * que tambem deixaria TenantContext/MDC vazarem para a proxima requisicao
 * processada pela mesma thread do Tomcat.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("{} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            TenantContext.clear();
            MDC.clear();
        }
    }
}
