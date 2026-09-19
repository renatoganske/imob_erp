package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

public class PayloadTooLargeException extends ApiException {

    public PayloadTooLargeException(String message) {
        super(message, "PAYLOAD_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
