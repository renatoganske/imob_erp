package com.imobcrm.notification;

import com.imobcrm.notification.domain.EmailDispatcher;
import com.imobcrm.notification.domain.EmailMessage;
import com.imobcrm.notification.domain.EmailOutboxService;
import com.imobcrm.notification.domain.EmailSendException;
import com.imobcrm.notification.domain.EmailSender;
import com.imobcrm.notification.domain.EmailTemplate;
import com.imobcrm.support.IntegrationTestBase;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/** Outbox de e-mail (IMOB-36): atomicidade com o fato de negocio, envio, retry com backoff e falha definitiva. */
class EmailOutboxTest extends IntegrationTestBase {

    private static UUID tenantId;

    @Autowired
    private EmailOutboxService outboxService;
    @Autowired
    private EmailDispatcher dispatcher;
    @Autowired
    private TransactionTemplate tx;
    @MockBean
    private EmailSender emailSender;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Outbox', 'imob-email-outbox', 'BASIC')", tenantId);
    }

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM email_outbox");
        reset(emailSender);
        TenantContext.set(new TenantContext.RequestPrincipal(tenantId, UUID.randomUUID(), "eo_admin", Role.ADMIN));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UUID enqueueInTx() {
        return tx.execute(status -> outboxService.enqueue(
                EmailTemplate.USER_INVITE, "corretor@x.com", Map.of("name", "Ana", "actionUrl", "https://app/aceitar")));
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM email_outbox WHERE id = ?", String.class, id);
    }

    private int attemptsOf(UUID id) {
        return jdbc.queryForObject("SELECT attempts FROM email_outbox WHERE id = ?", Integer.class, id);
    }

    @Test
    void enqueueWithoutTransactionIsRejected() {
        assertThrows(IllegalTransactionStateException.class, () -> outboxService.enqueue(
                EmailTemplate.USER_INVITE, "corretor@x.com", Map.of("name", "Ana", "actionUrl", "u")));
    }

    @Test
    void rollbackOfTheBusinessTransactionDiscardsTheEmail() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            outboxService.enqueue(EmailTemplate.USER_INVITE, "corretor@x.com", Map.of("name", "Ana", "actionUrl", "u"));
            throw new IllegalStateException("falha de negocio");
        }));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM email_outbox", Integer.class));
        assertEquals(0, dispatcher.dispatchDue());
        verify(emailSender, never()).send(any());
    }

    @Test
    void pendingEmailIsRenderedSentAndMarkedSent() {
        UUID id = enqueueInTx();
        assertEquals("PENDING", statusOf(id));

        assertEquals(1, dispatcher.dispatchDue());

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(sent.capture());
        assertEquals("corretor@x.com", sent.getValue().to());
        assertTrue(sent.getValue().body().contains("Ana"));
        assertTrue(sent.getValue().body().contains("https://app/aceitar"));
        assertEquals("SENT", statusOf(id));
        assertNotNull(jdbc.queryForObject("SELECT sent_at FROM email_outbox WHERE id = ?", OffsetDateTime.class, id));

        assertEquals(0, dispatcher.dispatchDue());
    }

    @Test
    void failureIncrementsAttemptsAndSchedulesRetryWithBackoff() {
        UUID id = enqueueInTx();
        doThrow(new EmailSendException("smtp fora do ar", null)).when(emailSender).send(any());

        assertEquals(1, dispatcher.dispatchDue());

        assertEquals("PENDING", statusOf(id));
        assertEquals(1, attemptsOf(id));
        assertEquals("smtp fora do ar", jdbc.queryForObject("SELECT last_error FROM email_outbox WHERE id = ?", String.class, id));
        OffsetDateTime next = jdbc.queryForObject("SELECT next_attempt_at FROM email_outbox WHERE id = ?", OffsetDateTime.class, id);
        assertTrue(next.isAfter(OffsetDateTime.now()));

        // ainda nao venceu: o dispatcher nao tenta de novo
        assertEquals(0, dispatcher.dispatchDue());
    }

    @Test
    void emailBecomesFailedAfterMaxAttempts() {
        UUID id = enqueueInTx();
        doThrow(new EmailSendException("smtp fora do ar", null)).when(emailSender).send(any());

        for (int i = 1; i <= 5; i++) {
            assertEquals(1, dispatcher.dispatchDue());
            if (i < 5) {
                jdbc.update("UPDATE email_outbox SET next_attempt_at = now() - interval '1 hour' WHERE id = ?", id);
            }
        }

        assertEquals("FAILED", statusOf(id));
        assertEquals(5, attemptsOf(id));
        assertNull(jdbc.queryForObject("SELECT sent_at FROM email_outbox WHERE id = ?", OffsetDateTime.class, id));
        assertEquals(0, dispatcher.dispatchDue());
    }

    @Test
    void recoveredProviderSendsTheEmailOnRetry() {
        UUID id = enqueueInTx();
        doThrow(new EmailSendException("instavel", null)).doNothing().when(emailSender).send(any());

        dispatcher.dispatchDue();
        jdbc.update("UPDATE email_outbox SET next_attempt_at = now() - interval '1 hour' WHERE id = ?", id);
        dispatcher.dispatchDue();

        assertEquals("SENT", statusOf(id));
        assertEquals(1, attemptsOf(id));
    }
}
