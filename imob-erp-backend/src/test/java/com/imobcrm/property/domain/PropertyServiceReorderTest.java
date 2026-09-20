package com.imobcrm.property.domain;

import com.imobcrm.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regra pura de reordenacao de fotos (IMOB-50), sem Spring nem banco. */
class PropertyServiceReorderTest {

    private static final List<String> URLS = List.of("http://r2/t/p/a.png", "http://r2/t/p/b.png", "http://r2/t/p/c.png");

    @Test
    void returnsTheUrlsInTheOrderOfTheKeys() {
        assertThat(PropertyService.reordered(URLS, List.of("c.png", "a.png", "b.png")))
                .containsExactly("http://r2/t/p/c.png", "http://r2/t/p/a.png", "http://r2/t/p/b.png");
    }

    @Test
    void sameOrderIsANoOp() {
        assertThat(PropertyService.reordered(URLS, List.of("a.png", "b.png", "c.png"))).containsExactlyElementsOf(URLS);
    }

    @Test
    void emptyGalleryAcceptsAnEmptyList() {
        assertThat(PropertyService.reordered(List.of(), List.of())).isEmpty();
    }

    @Test
    void rejectsMissingExtraDuplicatedOrUnknownKeys() {
        List.of(
                List.of("a.png", "b.png"),
                List.of("a.png", "b.png", "c.png", "a.png"),
                List.of("a.png", "b.png", "b.png"),
                List.of("a.png", "b.png", "x.png"),
                List.<String>of()
        ).forEach(keys -> assertThatThrownBy(() -> PropertyService.reordered(URLS, keys))
                .isInstanceOf(BusinessException.class));
    }

    @Test
    void doesNotMutateTheInputList() {
        List<String> input = new java.util.ArrayList<>(URLS);
        PropertyService.reordered(input, List.of("c.png", "b.png", "a.png"));
        assertThat(input).containsExactlyElementsOf(URLS);
    }
}
