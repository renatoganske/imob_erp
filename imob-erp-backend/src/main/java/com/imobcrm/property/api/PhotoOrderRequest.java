package com.imobcrm.property.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Chaves das fotos (ultimo segmento da URL) na nova ordem; a primeira e a capa. */
public record PhotoOrderRequest(@NotNull List<@NotBlank String> keys) {
}
