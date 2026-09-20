package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

/** Falha em um servico externo (ex.: Clerk): 502 para o chamador saber que pode repetir a operacao. */
public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message, String code) {
        super(message, code, HttpStatus.BAD_GATEWAY);
    }
}
