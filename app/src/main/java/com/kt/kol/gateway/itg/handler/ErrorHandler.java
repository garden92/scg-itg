package com.kt.kol.gateway.itg.handler;

import java.nio.charset.StandardCharsets;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.gateway.itg.exception.InvalidRequestException;
import com.kt.kol.gateway.itg.exception.SoapServiceException;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ErrorHandler {
    
    private final ObjectMapper objectMapper;
    public Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        log.error("Error processing request", error);
        exchange.getResponse().setStatusCode(determineHttpStatus(error));
        return writeErrorResponse(exchange, error);
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof InvalidRequestException) {
            return HttpStatus.BAD_REQUEST;
        } else if (error instanceof SoapServiceException) {
            return HttpStatus.BAD_GATEWAY;
        } else if (error instanceof ConversionException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, Throwable error) {
        ErrorResponse errorResponse = new ErrorResponse(
                error.getMessage(),
                error.getClass().getSimpleName(),
                "11");

        try {
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(errorJson.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    @Data
    static class ErrorResponse {
        private final String message;
        private final String error;
        private final String timestamp;
    }

}
