package com.imobcrm.config;

import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.User;
import com.imobcrm.user.domain.UserRepository;
import com.imobcrm.user.domain.enums.Role;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClerkJwtAuthenticationFilterTest {

    private static final String CLERK_USER_ID = "user_2abc";

    @Mock
    private DefaultJWTProcessor<SecurityContext> jwtProcessor;

    @Mock
    private UserRepository userRepository;

    private ClerkJwtAuthenticationFilter filter;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        filter = new ClerkJwtAuthenticationFilter(jwtProcessor, userRepository);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(CLERK_USER_ID)
                .claim("publicMetadata", Map.of("tenantId", tenantId.toString(), "role", "CORRETOR"))
                .build();
        lenient().when(jwtProcessor.process(any(SignedJWT.class), isNull())).thenReturn(claims);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesWithLocalUserIdRatherThanADerivedOne() throws Exception {
        User user = user(tenantId, true);
        when(userRepository.findByClerkUserId(CLERK_USER_ID)).thenReturn(Optional.of(user));

        runFilter();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        var principal = (TenantContext.RequestPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(user.getId());
        assertThat(principal.tenantId()).isEqualTo(tenantId);
        assertThat(principal.role()).isEqualTo(Role.CORRETOR);
    }

    @Test
    void doesNotAuthenticateWhenUserIsNotRegisteredLocally() throws Exception {
        when(userRepository.findByClerkUserId(CLERK_USER_ID)).thenReturn(Optional.empty());

        runFilter();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticateInactiveUser() throws Exception {
        when(userRepository.findByClerkUserId(CLERK_USER_ID)).thenReturn(Optional.of(user(tenantId, false)));

        runFilter();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticateWhenTokenTenantDiffersFromUserTenant() throws Exception {
        when(userRepository.findByClerkUserId(CLERK_USER_ID)).thenReturn(Optional.of(user(UUID.randomUUID(), true)));

        runFilter();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void failsFastWhenIssuerIsNotConfigured() {
        var properties = new ClerkProperties("sk", "http://localhost/jwks", " ", List.of());

        assertThatThrownBy(() -> new ClerkJwtAuthenticationFilter(properties, userRepository, (clerkUserId, tenantId) -> java.util.Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLERK_ISSUER");
    }

    private User user(UUID userTenantId, boolean active) {
        return User.builder()
                .id(UUID.randomUUID())
                .tenantId(userTenantId)
                .clerkUserId(CLERK_USER_ID)
                .role(Role.CORRETOR)
                .active(active)
                .build();
    }

    private void runFilter() throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder().subject(CLERK_USER_ID).build());
        jwt.sign(new MACSigner(new byte[32]));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.serialize());
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
}
