package com.imobcrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "clerk")
public record ClerkProperties(String secretKey, String jwksUrl, String issuer, List<String> authorizedParties) {
}
