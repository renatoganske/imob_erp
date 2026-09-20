package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message, String code) {
        super(message, code, HttpStatus.CONFLICT);
    }
}
