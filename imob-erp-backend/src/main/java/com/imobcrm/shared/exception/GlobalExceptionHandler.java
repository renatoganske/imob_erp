package com.imobcrm.shared.exception;

import com.imobcrm.shared.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNIQUE_VIOLATION = "23505";
    /** Limite defensivo contra cadeias de causas circulares. */
    private static final int MAX_CAUSE_DEPTH = 20;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("{}: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getMessage(), ex.getCode()));
    }

    /** Negacao do @PreAuthorize: sem este handler ela cairia no handler generico e viraria 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acesso negado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Acesso negado", "FORBIDDEN"));
    }

    /** Limite do multipart estourado antes de chegar ao controller: mesmo contrato do UploadValidator (413). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("Arquivo acima do limite de 10MB", "PAYLOAD_TOO_LARGE"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dados invalidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message, "VALIDATION_ERROR"));
    }

    /** Parametro de query/path com formato invalido (ex.: month=2026-13): erro do cliente, nao 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Parametro '" + ex.getName() + "' invalido", "VALIDATION_ERROR"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade de dados", ex);
        if (isUniqueViolation(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Registro duplicado: ja existe um registro com estes dados", "DUPLICATE_RESOURCE"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Dados invalidos: verifique as referencias informadas", "DATA_INTEGRITY_VIOLATION"));
    }

    /** SQLSTATE 23505 (unique_violation) em qualquer ponto da cadeia de causas: corrida entre duas requisicoes iguais. */
    static boolean isUniqueViolation(Throwable error) {
        return Stream.iterate(error, Objects::nonNull, Throwable::getCause)
                .limit(MAX_CAUSE_DEPTH)
                .anyMatch(cause -> cause instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro interno inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno inesperado", "INTERNAL_ERROR"));
    }
}
