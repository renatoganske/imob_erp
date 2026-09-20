package com.imobcrm.user.domain;

import com.imobcrm.onboarding.ClerkUserDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Aceite de convite (IMOB-40): o tenantId vem do publicMetadata do token, gravado pelo backend ao
 * convidar. So vincula um registro pendente DAQUELE tenant cujo e-mail esteja VERIFICADO na conta do
 * Clerk; qualquer outra coisa segue nao autenticada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteAcceptanceService implements PendingInviteLinker {

    private final UserRepository userRepository;
    private final ClerkUserDirectory clerk;

    @Override
    @Transactional
    public Optional<User> linkPendingInvite(String clerkUserId, UUID tenantId) {
        List<User> pending = userRepository.findPendingInvites(tenantId);
        if (pending.isEmpty()) {
            return Optional.empty(); // caminho comum: nem chama o Clerk
        }
        Set<String> verifiedEmails = clerk.findVerifiedEmails(clerkUserId);
        for (User invite : pending) {
            if (verifiedEmails.contains(invite.getEmail().toLowerCase(Locale.ROOT))) {
                invite.setClerkUserId(clerkUserId);
                invite.setActive(true);
                User linked = userRepository.save(invite);
                log.info("Convite aceito: user={} tenant={} role={}", linked.getId(), tenantId, linked.getRole());
                return Optional.of(linked);
            }
        }
        return Optional.empty();
    }
}
