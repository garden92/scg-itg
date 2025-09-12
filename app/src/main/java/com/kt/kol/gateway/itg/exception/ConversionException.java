package com.kt.kol.gateway.itg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ConversionException extends RuntimeException {
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
