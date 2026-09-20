package com.imobcrm.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void multipartLimitExceededIs413() {
        var response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10L * 1024 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().code()).isEqualTo("PAYLOAD_TOO_LARGE");
    }

    @Test
    void accessDeniedIs403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("negado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void uniqueViolationIs409() {
        var duplicate = new DataIntegrityViolationException("dup", new SQLException("duplicate key", "23505"));

        var response = handler.handleDataIntegrityViolation(duplicate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    void uniqueViolationIsFoundDeepInTheCauseChain() {
        var wrapped = new DataIntegrityViolationException("dup",
                new RuntimeException("hibernate", new SQLException("duplicate key", "23505")));

        assertThat(GlobalExceptionHandler.isUniqueViolation(wrapped)).isTrue();
    }

    @Test
    void otherIntegrityViolationsStay400() {
        var foreignKey = new DataIntegrityViolationException("fk", new SQLException("fk violation", "23503"));

        var response = handler.handleDataIntegrityViolation(foreignKey);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void uploadExceptionsCarryTheirHttpStatus() {
        assertThat(handler.handleApiException(new UnsupportedMediaTypeException("x")).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(handler.handleApiException(new PayloadTooLargeException("x")).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
