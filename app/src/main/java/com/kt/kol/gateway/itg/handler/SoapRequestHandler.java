package com.kt.kol.gateway.itg.handler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.gateway.itg.exception.InvalidRequestException;
import com.kt.kol.gateway.itg.exception.SoapServiceException;
import com.kt.kol.gateway.itg.metrics.PerformanceMetrics;
import com.kt.kol.common.constant.MediaTypes;
import com.kt.kol.gateway.itg.service.RequestValidationService;
import com.kt.kol.gateway.itg.service.ResponseWriterService;
import com.kt.kol.gateway.itg.service.SoapProcessingService;
import io.micrometer.core.instrument.Timer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoapRequestHandler {

    private final ObjectMapper objectMapper;
    private final PerformanceMetrics performanceMetrics;
    private final RequestValidationService validationService;
    private final SoapProcessingService processingService;
    private final ResponseWriterService writerService;

    /**
     * 최적화된 핸들러 - 새로운 서비스 사용
     * SOAP 헤더 정보가 포함된 강화된 Exchange 사용
     */
    public Mono<Void> handleRequest(ServerWebExchange exchange) {
        Timer.Sample sample = performanceMetrics.startSoapRequest();

        return validationService.extractAndValidateRequest(exchange)
                .flatMap(validatedRequest -> {
                    // 강화된 Exchange를 사용하여 SOAP 처리
                    return processingService.processSoapRequest(
                            validatedRequest.exchange(), // 강화된 Exchange 사용
                            validatedRequest.request()
                    ).flatMap(response -> 
                            writerService.writeResponse(validatedRequest.exchange(), response)
                    );
                })
                .doOnSuccess(result -> performanceMetrics.recordSoapSuccess(sample))
                .onErrorResume(error -> {
                    String errorType = error.getClass().getSimpleName();
                    performanceMetrics.recordSoapError(sample, errorType);
                    return handleError(exchange, error);
                });
    }


    /**
     * 에러 처리 메소드
     */
    private Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        log.error("Error processing SOAP request", error);

        // HTTP 상태 코드 설정
        HttpStatus httpStatus = determineHttpStatus(error);
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().add(MediaTypes.HEADER_CONTENT_TYPE, MediaTypes.APPLICATION_JSON_UTF8);

        // 에러 응답 생성
        try {
            String errorJson = createErrorJson(error);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(errorJson.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to create error response", e);
            return exchange.getResponse().setComplete();
        }
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

    private String createErrorJson(Throwable error) throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error.getClass().getSimpleName());
        errorResponse.put("message", error.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return objectMapper.writeValueAsString(errorResponse);
    }
}