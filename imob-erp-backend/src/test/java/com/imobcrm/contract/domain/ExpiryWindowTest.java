package com.imobcrm.contract.domain;

import com.imobcrm.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpiryWindowTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);

    @Test
    void endingWithinSpansFromTodayToTodayPlusDays() {
        ExpiryWindow window = ExpiryWindow.endingWithin(TODAY, 30);

        assertThat(window.from()).isEqualTo(TODAY);
        assertThat(window.to()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void containsIncludesBothBoundaries() {
        ExpiryWindow window = ExpiryWindow.endingWithin(TODAY, 30);

        assertThat(window.contains(TODAY)).isTrue();
        assertThat(window.contains(TODAY.plusDays(30))).isTrue();
        assertThat(window.contains(TODAY.plusDays(31))).isFalse();
    }

    @Test
    void containsExcludesAlreadyEndedAndMissingEndDates() {
        ExpiryWindow window = ExpiryWindow.endingWithin(TODAY, 90);

        assertThat(window.contains(TODAY.minusDays(1))).isFalse();
        assertThat(window.contains(null)).isFalse();
    }

    @Test
    void endingWithinRejectsNonPositiveAndTooLargeWindows() {
        for (int days : new int[]{0, -1, 366}) {
            assertThatThrownBy(() -> ExpiryWindow.endingWithin(TODAY, days))
                    .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_EXPIRY_WINDOW"));
        }
        assertThat(ExpiryWindow.endingWithin(TODAY, 365).to()).isEqualTo(TODAY.plusDays(365));
    }
}
