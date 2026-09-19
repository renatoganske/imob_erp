package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

    public BusinessException(String message, String code) {
        super(message, code, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
