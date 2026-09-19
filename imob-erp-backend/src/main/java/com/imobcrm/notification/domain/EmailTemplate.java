package com.imobcrm.notification.domain;

import java.util.Map;

/**
 * Templates de e-mail versionados em codigo. Os placeholders {chave} sao substituidos
 * pelas variaveis gravadas na outbox; o dominio grava dados, nao HTML.
 */
public enum EmailTemplate {

    USER_INVITE(
            "Voce foi convidado para o Imob ERP",
            "Ola, {name}!\n\nVoce foi convidado(a) para acessar o Imob ERP. Para aceitar o convite, acesse:\n{actionUrl}\n");

    private final String subject;
    private final String body;

    EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public EmailMessage render(String to, Map<String, String> variables) {
        return new EmailMessage(to, replace(subject, variables), replace(body, variables));
    }

    private static String replace(String text, Map<String, String> variables) {
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
