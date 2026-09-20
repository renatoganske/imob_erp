package com.imobcrm.onboarding;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Porta para a Backend API do Clerk: so o que onboarding e convites precisam. */
public interface ClerkUserDirectory {

    /** Usuario do Clerk que possui este e-mail JA VERIFICADO (nunca vincula por e-mail nao verificado). */
    Optional<ClerkUser> findByVerifiedEmail(String email);

    /** E-mails verificados (minusculos) do usuario do Clerk. */
    Set<String> findVerifiedEmails(String clerkUserId);

    /** Mescla as chaves no publicMetadata do usuario (as demais chaves ficam intactas). */
    void mergePublicMetadata(String clerkUserId, Map<String, Object> metadata);

    /** Envia o convite por e-mail; o publicMetadata do convite passa para o usuario quando ele se cadastra. */
    void createInvitation(String email, Map<String, Object> publicMetadata, String redirectUrl);

    /** {@code tenantId} e o que ja consta no publicMetadata do usuario (null se nao houver). */
    record ClerkUser(String id, String name, String tenantId) {

        public ClerkUser(String id, String name) {
            this(id, name, null);
        }
    }
}
