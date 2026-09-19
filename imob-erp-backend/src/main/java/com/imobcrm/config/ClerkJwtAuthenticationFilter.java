package com.imobcrm.config;

import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.User;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Valida o JWT emitido pelo Clerk contra o endpoint JWKS configurado e
 * popula o SecurityContext com um RequestPrincipal (tenantId, userId, role)
 * extraido do claim publicMetadata, conforme especificado em specs.md#7.
 * O userId do principal e o id local (users.id), resolvido por clerk_user_id;
 * usuarios inexistentes, inativos ou de outro tenant nao sao autenticados.
 */
@Slf4j
public class ClerkJwtAuthenticationFilter extends OncePerRequestFilter {

    private final DefaultJWTProcessor<SecurityContext> jwtProcessor;
    private final UserRepository userRepository;

    public ClerkJwtAuthenticationFilter(ClerkProperties clerkProperties, UserRepository userRepository) {
        this(buildProcessor(clerkProperties), userRepository);
    }

    ClerkJwtAuthenticationFilter(DefaultJWTProcessor<SecurityContext> jwtProcessor, UserRepository userRepository) {
        this.jwtProcessor = jwtProcessor;
        this.userRepository = userRepository;
    }

    private static DefaultJWTProcessor<SecurityContext> buildProcessor(ClerkProperties properties) {
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("CLERK_ISSUER e obrigatorio para validar o issuer do JWT");
        }
        try {
            var jwkSource = JWKSourceBuilder.create(new URL(properties.jwksUrl())).build();
            var processor = new DefaultJWTProcessor<SecurityContext>();
            processor.setJWTClaimsSetVerifier(new ClerkJwtClaimsVerifier(
                    properties.issuer().replaceAll("/+$", ""),
                    properties.authorizedParties() == null ? List.of() : properties.authorizedParties()));
            var keySelector = new JWSVerificationKeySelector<SecurityContext>(
                    com.nimbusds.jose.JWSAlgorithm.RS256, jwkSource);
            processor.setJWSKeySelector(keySelector);
            return processor;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("CLERK_JWKS_URL invalido", e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, java.io.IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring("Bearer ".length());
            SignedJWT jwt = SignedJWT.parse(token);
            var claims = jwtProcessor.process(jwt, null);

            String clerkUserId = claims.getSubject();
            @SuppressWarnings("unchecked")
            Map<String, Object> publicMetadata = (Map<String, Object>) claims.getClaim("publicMetadata");
            if (publicMetadata == null) {
                throw new IllegalStateException("Token sem publicMetadata");
            }

            UUID tenantId = UUID.fromString((String) publicMetadata.get("tenantId"));
            Role role = Role.valueOf((String) publicMetadata.get("role"));
            User user = userRepository.findByClerkUserId(clerkUserId)
                    .orElseThrow(() -> new IllegalStateException("Usuario nao cadastrado localmente: " + clerkUserId));
            if (!user.isActive()) {
                throw new IllegalStateException("Usuario inativo: " + user.getId());
            }
            if (!user.getTenantId().equals(tenantId)) {
                throw new IllegalStateException("Tenant do token diferente do tenant do usuario: " + user.getId());
            }

            var principal = new TenantContext.RequestPrincipal(tenantId, user.getId(), clerkUserId, role);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("Falha na validacao do JWT do Clerk: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
