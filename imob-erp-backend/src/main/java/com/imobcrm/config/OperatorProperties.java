package com.imobcrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Chave do operador do produto (quem cria imobiliarias). Vazia = endpoints internos desligados. */
@ConfigurationProperties(prefix = "app.operator")
public record OperatorProperties(String apiKey) {
}
