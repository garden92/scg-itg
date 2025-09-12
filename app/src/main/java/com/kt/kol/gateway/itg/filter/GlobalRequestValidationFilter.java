package com.kt.kol.gateway.itg.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.kt.kol.gateway.itg.exception.InvalidRequestException;
import com.kt.kol.common.constant.ServiceConstants;

import reactor.core.publisher.Mono;

@Component
public class GlobalRequestValidationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!exchange.getRequest().getHeaders().getContentType()
                .includes(MediaType.APPLICATION_JSON)) {
            return Mono.error(new InvalidRequestException(ServiceConstants.VALIDATION_INVALID_CONTENT_TYPE));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 가장 먼저 실행되도록
    }
}
