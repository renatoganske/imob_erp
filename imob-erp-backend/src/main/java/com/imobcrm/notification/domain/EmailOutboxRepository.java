package com.imobcrm.notification.domain;

import java.time.OffsetDateTime;
import java.util.List;

public interface EmailOutboxRepository {

    <S extends EmailOutbox> S save(S entity);

    /**
     * Reserva um lote de e-mails PENDING vencidos com bloqueio de linha (SKIP LOCKED), para que
     * dispatchers concorrentes nunca peguem a mesma mensagem. Deve rodar dentro de uma transacao.
     */
    List<EmailOutbox> lockDue(OffsetDateTime now, int limit);
}
