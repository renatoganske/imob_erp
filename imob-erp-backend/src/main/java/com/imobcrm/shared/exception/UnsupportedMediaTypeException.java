package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedMediaTypeException extends ApiException {

    public UnsupportedMediaTypeException(String message) {
        super(message, "UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
