package com.imobcrm.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClerkJwtClaimsVerifierTest {

    private static final String ISSUER = "https://app.clerk.accounts.dev";

    private final ClerkJwtClaimsVerifier verifier =
            new ClerkJwtClaimsVerifier(ISSUER, List.of("http://localhost:3000", "https://app.imob.com"));

    @Test
    void accepts_validIssuerAndAuthorizedParty() {
        assertThatCode(() -> verifier.verify(claims(ISSUER, "http://localhost:3000", 300), null))
                .doesNotThrowAnyException();
    }

    @Test
    void accepts_tokenWithoutAzp() {
        assertThatCode(() -> verifier.verify(claims(ISSUER, null, 300), null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejects_wrongIssuer() {
        assertThatThrownBy(() -> verifier.verify(claims("https://evil.example.com", null, 300), null))
                .isInstanceOf(BadJWTException.class);
    }

    @Test
    void rejects_missingIssuer() {
        assertThatThrownBy(() -> verifier.verify(claims(null, null, 300), null))
                .isInstanceOf(BadJWTException.class);
    }

    @Test
    void rejects_unauthorizedParty() {
        assertThatThrownBy(() -> verifier.verify(claims(ISSUER, "https://evil.example.com", 300), null))
                .isInstanceOf(BadJWTException.class)
                .hasMessageContaining("azp");
    }

    @Test
    void rejects_expiredToken() {
        assertThatThrownBy(() -> verifier.verify(claims(ISSUER, null, -3600), null))
                .isInstanceOf(BadJWTException.class);
    }

    private JWTClaimsSet claims(String issuer, String azp, long expiresInSeconds) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject("user_2abc")
                .expirationTime(Date.from(Instant.now().plusSeconds(expiresInSeconds)));
        if (issuer != null) {
            builder.issuer(issuer);
        }
        if (azp != null) {
            builder.claim("azp", azp);
        }
        return builder.build();
    }
}
