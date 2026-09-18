package com.imobcrm.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " nao encontrado(a): " + id, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
