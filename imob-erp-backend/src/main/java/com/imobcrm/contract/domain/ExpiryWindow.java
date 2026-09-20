package com.imobcrm.contract.domain;

import com.imobcrm.shared.exception.BusinessException;

import java.time.LocalDate;
import java.util.List;

/**
 * Janela de vencimento de um contrato de locacao (IMOB-28): do dia de hoje ate {@code days} dias adiante,
 * ambos inclusivos. Contratos que ja passaram do fim ficam fora: o alerta e sobre o que ainda vai vencer.
 */
public record ExpiryWindow(LocalDate from, LocalDate to) {

    /** Niveis exibidos no dashboard: 90 (info), 60 (amarelo) e 30 (vermelho). */
    public static final List<Integer> ALERT_DAYS = List.of(30, 60, 90);

    static final int MAX_DAYS = 365;

    public static ExpiryWindow endingWithin(LocalDate today, int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BusinessException("expiringInDays deve estar entre 1 e " + MAX_DAYS, "INVALID_EXPIRY_WINDOW");
        }
        return new ExpiryWindow(today, today.plusDays(days));
    }

    public boolean contains(LocalDate endDate) {
        return endDate != null && !endDate.isBefore(from) && !endDate.isAfter(to);
    }
}
