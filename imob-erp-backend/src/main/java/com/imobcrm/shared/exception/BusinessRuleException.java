package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message, String code) {
        super(message, code, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
