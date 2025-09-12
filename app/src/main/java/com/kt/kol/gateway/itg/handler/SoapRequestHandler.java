package com.kt.kol.gateway.itg.handler;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
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
    private final ErrorHandler errorHandler;
    private final SoapServiceProperies soapClientProperies;
    private final ObjectMapper objectMapper;
    private final EndpointStrategyResolver endpointStrategyResolver;

    public Mono<Void> handleRequest(ServerWebExchange exchange) {
        return Mono.just(exchange)
                .flatMap(this::extractRequestBody)
                .flatMap(body -> processRequest(exchange, body))
                .flatMap(response -> writeResponse(exchange, response))
                .onErrorResume(error -> errorHandler.handleError(exchange, error));
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
     * @deprecated Header 가 아닌 trtBaseInfo 로 변경
     */
    // @Deprecated
    // private String determineEndpoint(ServerWebExchange exchange) {
    //     HttpHeaders headers = exchange.getRequest().getHeaders();
    //     String serviceType = headers.getFirst(HeaderConstants.FN_NAME);

    //     // 비즈니스 로직에 따른 엔드포인트 결정
    //     if ("service".equals(serviceType)) {
    //         return soapClientProperies.getPoEndPoint();
    //     } else {
    //         return soapClientProperies.getEsbEndPoint();
    //     }
    // }
}
