package com.kt.kol.gateway.itg.service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
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
 * JSON/XML 요청 검증 및 파싱 서비스
 *
 * <p>
 * 주요 기능:
 * <ul>
 * <li>클라이언트로부터 받은 JSON/XML 요청을 파싱하여 RequestStdVO 객체로 변환</li>
 * <li>필수 필드(appName, svcName, fnName) 존재 여부 검증</li>
 * <li>Content-Type에 따른 적절한 문자셋 처리</li>
 * </ul>
 *
 * <p>
 * 처리 흐름:
 * 
 * <pre>
 * 1. JSON 요청 수신
 * 2. 요청 본문 파싱 (비동기 처리)
 * 3. 필수 필드 검증
 * 4. ValidatedRequest 객체 반환
 * </pre>
 *
 * @see RequestStdVO
 * @see ValidatedRequest
 * @see com.kt.kol.gateway.itg.util.SoapConverter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestValidationService {

    // ✅ JSON 전용 매퍼를 명시적으로 주입 고정
    private final ObjectMapper objectMapper;

    /** 요청 본문 추출 및 검증 */
    public Mono<ValidatedRequest> extractAndValidateRequest(ServerWebExchange exchange) {
        final String path = exchange.getRequest().getPath().toString();
        log.info("[RequestValidation] 시작 - Path: {}, Method: {}", path, exchange.getRequest().getMethod());

        // XML 명시 차단 (이 엔드포인트는 JSON만)
        MediaType ct = exchange.getRequest().getHeaders().getContentType();
        if (ct != null && (MediaType.APPLICATION_XML.includes(ct) || MediaType.TEXT_XML.includes(ct))) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only application/json is supported on this endpoint"));
        }

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .switchIfEmpty(Mono.error(new InvalidRequestException("Empty request body")))
                .flatMap(buf -> parseRequestBody(buf, exchange, path))
                .doOnNext(vr -> {
                    validateRequest(vr.request());
                    var info = vr.request().svcRequestInfoDTO();
                    log.info("[RequestValidation] 완료 - Service: {}/{}, App: {}",
                            info.svcName(), info.fnName(), info.appName());
                })
                .doOnError(e -> {
                    if (e instanceof InvalidRequestException || e instanceof ResponseStatusException) {
                        log.warn("[RequestValidation] 실패 - Path: {}, Error: {}", path, e.getMessage());
                    } else {
                        log.error("[RequestValidation] 예기치 못한 오류 - Path: {}", path, e);
                    }
                });
    }

    /** 본문 파싱(JSON 전용) */
    private Mono<ValidatedRequest> parseRequestBody(DataBuffer dataBuffer, ServerWebExchange exchange, String path) {
        return Mono.fromCallable(() -> {
            byte[] bytes = null;
            try {
                bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                Charset charset = determineCharset(exchange);
                log.debug("[RequestValidation] 파싱 시작 - size={} bytes, charset={}, mapper={}",
                        bytes.length, charset.displayName(), objectMapper.getClass().getName());
                // 파싱
                RequestStdVO req = objectMapper.readValue(bytes, RequestStdVO.class);

                log.debug("[RequestValidation] 파싱 성공 - App: {}, Service: {}/{}",
                        (req.svcRequestInfoDTO() != null ? req.svcRequestInfoDTO().appName() : "N/A"),
                        (req.svcRequestInfoDTO() != null ? req.svcRequestInfoDTO().svcName() : "N/A"),
                        (req.svcRequestInfoDTO() != null ? req.svcRequestInfoDTO().fnName() : "N/A"));

                return ValidatedRequest.json(req, exchange);
            } catch (JsonProcessingException e) {
                throw new InvalidRequestException("Invalid JSON: " + e.getOriginalMessage());
            } catch (InvalidRequestException e) {
                throw e;
            } catch (Exception e) {
                throw new InvalidRequestException("Request processing failed: " + e.getMessage());
            } finally {
                DataBufferUtils.release(dataBuffer); // 누수 방지
                log.trace("[RequestValidation] DataBuffer released");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 필수 필드 검증 */
    private void validateRequest(RequestStdVO request) {
        log.trace("[RequestValidation] 필수 필드 검증 시작");

        if (request.svcRequestInfoDTO() == null)
            throw new InvalidRequestException("Missing service request info");

        var info = request.svcRequestInfoDTO();

        if (!StringUtils.hasText(info.appName()))
            throw new InvalidRequestException("Missing application name");

        if (!StringUtils.hasText(info.svcName()))
            throw new InvalidRequestException("Missing service name");

        if (!StringUtils.hasText(info.fnName()))
            throw new InvalidRequestException("Missing function name");

        log.debug("[RequestValidation] 검증 통과 - Service: {}/{}, App: {}",
                info.svcName(), info.fnName(), info.appName());
    }

    /** Content-Type의 charset 결정 (기본 UTF-8) */
    private Charset determineCharset(ServerWebExchange exchange) {
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        if (contentType != null && contentType.getCharset() != null) {
            Charset cs = contentType.getCharset();
            log.trace("[RequestValidation] charset: {}", cs.displayName());
            return cs;
        }
        log.trace("[RequestValidation] charset 미명시 - UTF-8 사용");
        return StandardCharsets.UTF_8;
    }
}