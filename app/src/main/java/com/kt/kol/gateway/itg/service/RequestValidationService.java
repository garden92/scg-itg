package com.kt.kol.gateway.itg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.gateway.itg.exception.InvalidRequestException;
import com.kt.kol.gateway.itg.model.RequestStdVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 요청 검증 서비스
 * 요청 데이터 추출 및 검증 로직 분리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestValidationService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 요청 본문 추출 및 검증
     * 메모리 효율적인 처리를 위한 최적화 적용
     */
    public Mono<RequestStdVO> extractAndValidateRequest(ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .switchIfEmpty(Mono.error(new InvalidRequestException("Empty request body")))
                .flatMap(this::parseRequestBody)
                .doOnNext(this::validateRequest)
                .doOnError(error -> log.error("Request validation failed", error));
    }
    
    /**
     * 요청 본문 파싱
     */
    private Mono<RequestStdVO> parseRequestBody(DataBuffer dataBuffer) {
        return Mono.fromCallable(() -> {
            try {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                String jsonString = new String(bytes, StandardCharsets.UTF_8);
                
                RequestStdVO request = objectMapper.readValue(jsonString, RequestStdVO.class);
                log.debug("Request parsed successfully");
                return request;
                
            } catch (JsonProcessingException e) {
                throw new InvalidRequestException("Invalid JSON format: " + e.getMessage());
            } finally {
                DataBufferUtils.release(dataBuffer);
            }
        });
    }
    
    /**
     * 요청 유효성 검증
     */
    private void validateRequest(RequestStdVO request) {
        // 필수 필드 검증
        if (request.svcRequestInfoDTO() == null) {
            throw new InvalidRequestException("Missing service request info");
        }
        
        if (request.svcRequestInfoDTO().appName() == null || 
            request.svcRequestInfoDTO().appName().isEmpty()) {
            throw new InvalidRequestException("Missing application name");
        }
        
        if (request.svcRequestInfoDTO().svcName() == null || 
            request.svcRequestInfoDTO().svcName().isEmpty()) {
            throw new InvalidRequestException("Missing service name");
        }
        
        if (request.svcRequestInfoDTO().fnName() == null || 
            request.svcRequestInfoDTO().fnName().isEmpty()) {
            throw new InvalidRequestException("Missing function name");
        }
        
        log.debug("Request validation passed for service: {}/{}", 
                 request.svcRequestInfoDTO().svcName(), 
                 request.svcRequestInfoDTO().fnName());
    }
}