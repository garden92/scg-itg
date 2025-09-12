package com.kt.kol.gateway.itg.handler;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.common.model.SvcRequestInfoDTO;
import com.kt.kol.common.util.DomainConstants;
import com.kt.kol.gateway.itg.exception.InvalidRequestException;
import com.kt.kol.gateway.itg.exception.SoapServiceException;
import com.kt.kol.gateway.itg.model.RequestStdVO;
import com.kt.kol.gateway.itg.model.ResponseStdVO;
import com.kt.kol.gateway.itg.properties.HeaderConstants;
import com.kt.kol.gateway.itg.properties.SoapServiceProperies;
import com.kt.kol.gateway.itg.strategy.EndpointStrategyResolver;
import com.kt.kol.gateway.itg.util.SoapConverter;
import com.kt.kol.gateway.itg.metrics.PerformanceMetrics;
import com.kt.kol.common.constant.MediaTypes;
import com.kt.kol.gateway.itg.service.RequestValidationService;
import com.kt.kol.gateway.itg.service.ResponseWriterService;
import com.kt.kol.gateway.itg.service.SoapProcessingService;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoapRequestHandler {

    private final WebClient webClient;
    private final SoapConverter soapConverter;

    private final SoapServiceProperies soapClientProperies;
    private final ObjectMapper objectMapper;
    private final EndpointStrategyResolver endpointStrategyResolver;
    private final PerformanceMetrics performanceMetrics;
    
    // 새로운 서비스 추가
    private final RequestValidationService validationService;
    private final SoapProcessingService processingService;
    private final ResponseWriterService writerService;
    
    // 기능 플래그 - 점진적 마이그레이션을 위한 설정
    @Value("${feature.use-optimized-handler:false}")
    private boolean useOptimizedHandler;

    /**
     * 통합된 핸들러 - 기능 플래그에 따라 새로운 서비스 또는 기존 로직 사용
     */
    public Mono<Void> handleRequest(ServerWebExchange exchange) {
        if (useOptimizedHandler) {
            return handleRequestOptimized(exchange);
        } else {
            return handleRequestLegacy(exchange);
        }
    }
    
    /**
     * 최적화된 핸들러 - 새로운 서비스 사용
     */
    private Mono<Void> handleRequestOptimized(ServerWebExchange exchange) {
        Timer.Sample sample = performanceMetrics.startSoapRequest();
        
        return validationService.extractAndValidateRequest(exchange)
                .flatMap(request -> processingService.processSoapRequest(exchange, request))
                .flatMap(response -> writerService.writeResponse(exchange, response))
                .doOnSuccess(result -> performanceMetrics.recordSoapSuccess(sample))
                .onErrorResume(error -> {
                    String errorType = error.getClass().getSimpleName();
                    performanceMetrics.recordSoapError(sample, errorType);
                    return handleError(exchange, error);
                });
    }
    
    /**
     * 기존 핸들러 로직 - 하위 호환성 유지
     */
    private Mono<Void> handleRequestLegacy(ServerWebExchange exchange) {
        Timer.Sample sample = performanceMetrics.startSoapRequest();
        
        return extractRequestBody(exchange)
                .flatMap(body -> processRequest(exchange, body))
                .flatMap(response -> writeResponse(exchange, response))
                .doOnSuccess(result -> performanceMetrics.recordSoapSuccess(sample))
                .onErrorResume(error -> {
                    String errorType = error.getClass().getSimpleName();
                    performanceMetrics.recordSoapError(sample, errorType);
                    return handleError(exchange, error);
                });
    }

    private Mono<RequestStdVO> extractRequestBody(ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .switchIfEmpty(Mono.error(new InvalidRequestException("Empty request body")))
                .map(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        String jsonString = new String(bytes, StandardCharsets.UTF_8);
                        return objectMapper.readValue(jsonString, RequestStdVO.class);
                    } catch (JsonProcessingException e) {
                        throw new InvalidRequestException("Invalid JSON format: " + e.getMessage());
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                });
    }

    // 매개변수 TrtBaseInfoDTO -> RequestInfoDTO 로 변경
    private Mono<ResponseStdVO> processRequest(ServerWebExchange exchange, RequestStdVO requestStdVO) {

        try {
            log.info("::requestStdVO:: {}", requestStdVO);
            String soapRequest = soapConverter.convertToSoap(exchange, requestStdVO);
            log.info("::soapRequest:: {}", soapRequest);
            String endpoint = endpointStrategyResolver.resolveEndpoint(
                requestStdVO.svcRequestInfoDTO(), 
                exchange.getRequest().getHeaders());
                
            WebClient.RequestBodySpec requestSpec = webClient.post().uri(endpoint);
            if (soapClientProperies.getStubEndPoint().equals(endpoint)) {
                String cmpnCd = exchange.getRequest().getHeaders().getFirst(HeaderConstants.CMPN_CD);
                requestSpec = requestSpec.header(HeaderConstants.CMPN_CD, cmpnCd);
            }
            return requestSpec
                    .bodyValue(soapRequest)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new SoapServiceException(
                                            "SOAP service error: " + response.statusCode(), error))))
                    .bodyToMono(String.class)
                    .map(res -> soapConverter.convertToStdVO(res))
                    .timeout(Duration.ofMillis(soapClientProperies.getTimeout()));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to process request", e));
        }
    }


    private Mono<Void> writeResponse(ServerWebExchange exchange, ResponseStdVO response) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize response", e));
        }
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
