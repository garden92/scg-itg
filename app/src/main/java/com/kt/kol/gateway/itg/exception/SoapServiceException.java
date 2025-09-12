package com.kt.kol.gateway.itg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class SoapServiceException extends RuntimeException {
    private final String soapError;

    public SoapServiceException(String message, String soapError) {
        super(message);
        this.soapError = soapError;
    }
}