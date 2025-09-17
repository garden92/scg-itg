package com.kt.kol.gateway.itg.service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.gateway.itg.exception.InvalidRequestException;
import com.kt.kol.gateway.itg.model.RequestStdVO;
import com.kt.kol.gateway.itg.model.ValidatedRequest;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 요청 검증 서비스
 * JSON 요청 처리 및 검증
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestValidationService {

    private final ObjectMapper objectMapper;

    /**
     * 요청 본문 추출 및 검증
     * JSON 요청 처리
     */
    public Mono<ValidatedRequest> extractAndValidateRequest(ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .switchIfEmpty(Mono.error(new InvalidRequestException("Empty request body")))
                .flatMap(buf -> parseRequestBody(buf, exchange))
                .doOnNext(vr -> validateRequest(vr.request()))
                .doOnError(e -> log.error("Request validation failed", e));
    }

    /**
     * 요청 본문 파싱 - Content-Type에 따라 JSON 또는 SOAP XML 처리
     * CPU 집약적 작업을 boundedElastic 스케줄러에서 실행하여 이벤트 루프 블로킹 방지
     */
    private Mono<ValidatedRequest> parseRequestBody(DataBuffer dataBuffer, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            try {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);

                // Content-Type에서 문자셋 결정
                Charset charset = determineCharset(exchange);
                String bodyString = new String(bytes, charset);

                log.debug("Processing JSON request");
                RequestStdVO request = objectMapper.readValue(bodyString, RequestStdVO.class);
                log.debug("Request parsed successfully");

                return ValidatedRequest.json(request, exchange);

            } catch (JsonProcessingException e) {
                log.warn("JSON parsing failed: {}", e.getMessage());
                throw new InvalidRequestException("Invalid JSON format: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected parsing error", e);
                throw new InvalidRequestException("Request processing failed: " + e.getMessage());
            } finally {
                DataBufferUtils.release(dataBuffer);
            }
        })
                .subscribeOn(Schedulers.boundedElastic()); // CPU 집약적 작업을 별도 스레드풀로 이동
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

    /**
     * Content-Type 헤더에서 문자셋을 결정
     * Content-Type에 charset이 명시되어 있으면 해당 charset 사용, 없으면 UTF-8 기본값 사용
     */
    private Charset determineCharset(ServerWebExchange exchange) {
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        if (contentType != null && contentType.getCharset() != null) {
            log.debug("Using charset from Content-Type: {}", contentType.getCharset());
            return contentType.getCharset();
        }
        log.debug("Using default charset: UTF-8");
        return StandardCharsets.UTF_8;
    }
}