package com.imobcrm.notification.domain;

import com.imobcrm.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository outboxRepository;

    /**
     * Enfileira um e-mail na outbox. Exige transacao ativa (MANDATORY): a linha so existe se o fato de
     * negocio que a originou for confirmado, e some junto em caso de rollback.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueue(EmailTemplate template, String to, Map<String, String> variables) {
        EmailOutbox email = EmailOutbox.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .toAddress(to)
                .template(template)
                .variables(Map.copyOf(variables))
                .status(EmailStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(OffsetDateTime.now())
                .build();
        return outboxRepository.save(email).getId();
    }
}
