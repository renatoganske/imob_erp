package com.imobcrm.config;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

import java.util.Collection;
import java.util.Set;

/**
 * Alem de exp/nbf, exige sub e o issuer configurado (Frontend API do Clerk) e,
 * quando o claim azp esta presente, que ele esteja entre as origens autorizadas.
 * O Clerk omite azp em alguns fluxos (ex.: sem header Origin), por isso a ausencia e aceita.
 */
public class ClerkJwtClaimsVerifier extends DefaultJWTClaimsVerifier<SecurityContext> {

    private final Set<String> authorizedParties;

    public ClerkJwtClaimsVerifier(String issuer, Collection<String> authorizedParties) {
        super(new JWTClaimsSet.Builder().issuer(issuer).build(), Set.of("sub", "exp"));
        this.authorizedParties = Set.copyOf(authorizedParties);
    }

    @Override
    public void verify(JWTClaimsSet claimsSet, SecurityContext context) throws BadJWTException {
        super.verify(claimsSet, context);
        Object azp = claimsSet.getClaim("azp");
        if (azp != null && !authorizedParties.isEmpty() && !authorizedParties.contains(azp.toString())) {
            throw new BadJWTException("JWT azp nao autorizado: " + azp);
        }
    }
}
