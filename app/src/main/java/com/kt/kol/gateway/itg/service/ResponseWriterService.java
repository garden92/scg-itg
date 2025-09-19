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

/**
 * JSON 응답 작성 서비스
 *
 * <p>ESB로부터 받은 SOAP 응답을 JSON 형태로 클라이언트에 전달
 *
 * @see ResponseStdVO
 * @see SoapProcessingService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseWriterService {

    private final ObjectMapper objectMapper;

    /**
     * JSON 응답 작성
     *
     * <p>ResponseStdVO 객체를 JSON으로 직렬화하여 클라이언트에 전달
     *
     * @param exchange ServerWebExchange 객체
     * @param response 응답 데이터
     * @return 응답 작성 완료 Mono
     */
    public Mono<Void> writeResponse(ServerWebExchange exchange, ResponseStdVO response) {
        String requestPath = exchange.getRequest().getPath().toString();

        try {
            // Content-Type을 JSON으로 설정
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            // ResponseStdVO를 JSON으로 직렬화
            byte[] responseBytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);

            log.info("[ResponseWriter] JSON 응답 작성 - Path: {}, ResponseType: {}, ResponseCode: {}",
                    requestPath,
                    response.responseType(),
                    response.responseCode());

            log.debug("[ResponseWriter] 응답 상세 - Title: {}, System: {}",
                    response.responseTitle(),
                    response.responseSystem());

            return exchange.getResponse().writeWith(Mono.just(buffer));

        } catch (JsonProcessingException e) {
            log.error("[ResponseWriter] JSON 직렬화 실패 - Path: {}, Error: {}",
                    requestPath, e.getMessage());
            return Mono.error(new RuntimeException("Failed to serialize JSON response", e));
        } catch (Exception e) {
            log.error("[ResponseWriter] 응답 작성 실패 - Path: {}", requestPath, e);
            return Mono.error(new RuntimeException("Failed to write response", e));
        }
    }
}