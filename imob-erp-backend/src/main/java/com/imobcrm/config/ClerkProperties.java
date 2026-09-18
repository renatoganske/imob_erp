package com.imobcrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clerk")
public record ClerkProperties(String secretKey, String jwksUrl) {
}
