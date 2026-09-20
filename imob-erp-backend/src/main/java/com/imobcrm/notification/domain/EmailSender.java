package com.imobcrm.notification.domain;

/** Porta de saida: o dominio nao conhece o provedor (SMTP, Resend, SES...). */
public interface EmailSender {

    /** @throws EmailSendException quando o provedor recusa ou fica indisponivel */
    void send(EmailMessage message);
}
