package com.kt.kol.gateway.itg.model;

import org.springframework.web.server.ServerWebExchange;

/**
 * 검증된 JSON 요청과 Exchange를 포함하는 결과 객체
 */
public record ValidatedRequest(
        RequestStdVO request,
        ServerWebExchange exchange
) {
    
    /**
     * JSON 요청용 생성자
     */
    public static ValidatedRequest json(RequestStdVO request, ServerWebExchange exchange) {
        return new ValidatedRequest(request, exchange);
    }
}