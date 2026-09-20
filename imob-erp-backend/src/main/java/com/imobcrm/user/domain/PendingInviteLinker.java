package com.imobcrm.user.domain;

import java.util.Optional;
import java.util.UUID;

/** Vincula quem acabou de aceitar um convite ao registro local pendente do tenant. */
@FunctionalInterface
public interface PendingInviteLinker {

    Optional<User> linkPendingInvite(String clerkUserId, UUID tenantId);
}
