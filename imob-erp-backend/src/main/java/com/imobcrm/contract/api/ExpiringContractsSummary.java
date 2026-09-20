package com.imobcrm.contract.api;

/**
 * Contagem acumulada de locacoes ATIVAS que vencem em ate 30, 60 e 90 dias (IMOB-28). Os numeros batem com
 * o total de {@code GET /contracts?expiringInDays=N}: um contrato que vence em 20 dias conta nos tres niveis.
 */
public record ExpiringContractsSummary(long within30Days, long within60Days, long within90Days) {
}
