package com.kt.kol.gateway.itg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.gateway.itg.model.ResponseStdVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 응답 작성 서비스
 * 응답 직렬화 및 전송 로직 분리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseWriterService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 응답 작성 및 전송
     * 비동기 직렬화 처리로 성능 최적화
     */
    public Mono<Void> writeResponse(ServerWebExchange exchange, ResponseStdVO response) {
        return Mono.fromCallable(() -> serializeResponse(response))
                .subscribeOn(Schedulers.boundedElastic()) // JSON 직렬화를 별도 스레드에서
                .flatMap(bytes -> sendResponse(exchange, bytes))
                .doOnSuccess(v -> log.debug("Response sent successfully"))
                .doOnError(error -> log.error("Failed to write response", error));
    }
    
    /**
     * 응답 직렬화
     */
    private byte[] serializeResponse(ResponseStdVO response) {
        try {
            return objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException e) {
            log.error("Response serialization failed", e);
            // 에러 응답 생성
            ResponseStdVO errorResponse = ResponseStdVO.systemError(
                    "SERIALIZATION_ERROR",
                    "Failed to serialize response",
                    "",
                    "",
                    "GATEWAY"
            );
            try {
                return objectMapper.writeValueAsBytes(errorResponse);
            } catch (JsonProcessingException ex) {
                // 최후의 폴백
                return "{\"error\":\"Internal server error\"}".getBytes();
            }
        }
    }
    
    /**
     * HTTP 응답 전송
     */
    private Mono<Void> sendResponse(ServerWebExchange exchange, byte[] responseBytes) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(responseBytes.length);
        
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}