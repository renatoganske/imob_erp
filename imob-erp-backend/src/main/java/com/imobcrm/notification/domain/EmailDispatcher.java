package com.imobcrm.notification.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Le a outbox e envia os e-mails pendentes. Falhas incrementam as tentativas com backoff
 * exponencial (1, 2, 4... minutos); ao estourar o maximo a mensagem vira FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatcher {

    static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final EmailOutboxRepository outboxRepository;
    private final EmailSender emailSender;

    @Value("${notification.email.dispatch-batch-size:" + BATCH_SIZE + "}")
    private int batchSize = BATCH_SIZE;

    @Scheduled(
            fixedDelayString = "${notification.email.dispatch-delay-ms:10000}",
            initialDelayString = "${notification.email.dispatch-initial-delay-ms:10000}")
    @Transactional
    public int dispatchDue() {
        OffsetDateTime now = OffsetDateTime.now();
        List<EmailOutbox> due = outboxRepository.lockDue(now, batchSize);
        due.forEach(email -> deliver(email, now));
        if (!due.isEmpty()) {
            log.info("Outbox de e-mail: {} mensagem(ns) processada(s)", due.size());
        }
        return due.size();
    }

    private void deliver(EmailOutbox email, OffsetDateTime now) {
        try {
            emailSender.send(email.getTemplate().render(email.getToAddress(), email.getVariables()));
            email.setStatus(EmailStatus.SENT);
            email.setSentAt(now);
            email.setLastError(null);
        } catch (RuntimeException e) {
            email.setAttempts(email.getAttempts() + 1);
            email.setLastError(truncate(e.getMessage()));
            if (email.getAttempts() >= MAX_ATTEMPTS) {
                email.setStatus(EmailStatus.FAILED);
                log.error("E-mail {} falhou definitivamente apos {} tentativas", email.getId(), email.getAttempts(), e);
            } else {
                email.setNextAttemptAt(now.plus(Duration.ofMinutes(1L << (email.getAttempts() - 1))));
                log.warn("E-mail {} falhou (tentativa {}): {}", email.getId(), email.getAttempts(), e.getMessage());
            }
        }
        outboxRepository.save(email);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
