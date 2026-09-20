package com.imobcrm.onboarding;

import java.util.Map;
import java.util.Optional;

/** Porta para a Backend API do Clerk: so o que o onboarding precisa. */
public interface ClerkUserDirectory {

    /** Usuario do Clerk que possui este e-mail JA VERIFICADO (nunca vincula por e-mail nao verificado). */
    Optional<ClerkUser> findByVerifiedEmail(String email);

    /** Mescla as chaves no publicMetadata do usuario (as demais chaves ficam intactas). */
    void mergePublicMetadata(String clerkUserId, Map<String, Object> metadata);

    record ClerkUser(String id, String name) {
    }
}
