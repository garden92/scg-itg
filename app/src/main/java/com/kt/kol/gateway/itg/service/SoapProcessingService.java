package com.kt.kol.gateway.itg.service;

import com.kt.kol.gateway.itg.model.RequestStdVO;
import com.kt.kol.gateway.itg.model.ResponseStdVO;
import com.kt.kol.gateway.itg.properties.SoapServiceProperties;
import com.kt.kol.gateway.itg.strategy.EndpointStrategyResolver;
import com.kt.kol.gateway.itg.util.SoapConverter;
import com.kt.kol.common.constant.HeaderConstants;
import com.kt.kol.gateway.itg.exception.SoapServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * SOAP 처리 비즈니스 로직 서비스
 * SoapRequestHandler에서 분리하여 책임 분리 원칙 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoapProcessingService {

    private final WebClient webClient;
    private final SoapConverter soapConverter;
    private final EndpointStrategyResolver endpointStrategyResolver;
    private final SoapServiceProperties soapServiceProperties;

    /**
     * SOAP 요청 처리 - 비동기 논블로킹 처리
     */
    public Mono<ResponseStdVO> processSoapRequest(ServerWebExchange exchange, RequestStdVO requestStdVO) {
        return Mono.fromCallable(() -> {
            // SOAP 변환 준비
            String soapRequest = soapConverter.convertToSoap(exchange, requestStdVO);
            String endpoint = endpointStrategyResolver.resolveEndpoint(
                    requestStdVO.svcRequestInfoDTO(),
                    exchange.getRequest().getHeaders());
            log.debug("Processing SOAP request to endpoint: {}", endpoint);
            return new SoapRequestContext(soapRequest, endpoint);
        })
                .subscribeOn(Schedulers.boundedElastic()) // CPU 집약적 작업을 별도 스레드에서
                .flatMap(context -> executeSoapCall(context, exchange))
                .map(soapConverter::convertToStdVO)
                .timeout(Duration.ofMillis(soapServiceProperties.getTimeout()))
                .doOnError(error -> log.error("SOAP processing failed", error));
    }

    /**
     * SOAP 호출 실행
     */
    private Mono<String> executeSoapCall(SoapRequestContext context, ServerWebExchange exchange) {
        WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri(context.endpoint());

        // Stub 엔드포인트 특별 처리
        if (isStubEndpoint(context.endpoint())) {
            requestSpec = addStubHeaders(requestSpec, exchange);
        }

        return requestSpec
                .bodyValue(context.soapRequest())
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(
                                        new SoapServiceException(
                                                "SOAP service error: " + response.statusCode(),
                                                error))))
                .bodyToMono(String.class);
    }

    /**
     * Stub 엔드포인트 확인
     */
    private boolean isStubEndpoint(String endpoint) {
        return soapServiceProperties.getStubEndPoint() != null &&
                soapServiceProperties.getStubEndPoint().equals(endpoint);
    }

    /**
     * Stub 헤더 추가
     */
    private WebClient.RequestBodySpec addStubHeaders(WebClient.RequestBodySpec spec,
            ServerWebExchange exchange) {
        String cmpnCd = exchange.getRequest().getHeaders().getFirst(HeaderConstants.CMPN_CD);
        if (cmpnCd != null) {
            return spec.header(HeaderConstants.CMPN_CD, cmpnCd);
        }
        return spec;
    }

    /**
     * SOAP 요청 컨텍스트
     */
    private record SoapRequestContext(String soapRequest, String endpoint) {
    }
}