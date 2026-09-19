package com.imobcrm.notification.domain;

public record EmailMessage(String to, String subject, String body) {
}
