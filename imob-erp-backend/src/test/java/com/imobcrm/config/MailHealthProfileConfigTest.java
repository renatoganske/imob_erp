package com.imobcrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IMOB-42: o health de e-mail so pode ser desligado no perfil dev (Mailpit costuma estar parado);
 * nos demais perfis o indicador continua ligado (default do Spring Boot).
 */
class MailHealthProfileConfigTest {

    private static final String MAIL_HEALTH_ENABLED = "management.health.mail.enabled";

    private static Optional<Object> mailHealthEnabled(String yamlFile) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(yamlFile, new ClassPathResource(yamlFile));
        return sources.stream()
                .map(source -> source.getProperty(MAIL_HEALTH_ENABLED))
                .filter(value -> value != null)
                .findFirst();
    }

    @Test
    void devProfileDisablesMailHealthIndicator() throws IOException {
        assertThat(mailHealthEnabled("application-dev.yml")).contains(false);
    }

    @Test
    void baseAndProdProfilesKeepMailHealthIndicatorEnabled() throws IOException {
        assertThat(mailHealthEnabled("application.yml")).isEmpty();
        assertThat(mailHealthEnabled("application-prod.yml")).isEmpty();
    }
}
