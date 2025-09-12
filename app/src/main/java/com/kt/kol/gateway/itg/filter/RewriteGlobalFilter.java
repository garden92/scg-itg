package com.kt.kol.gateway.itg.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import com.kt.kol.gateway.itg.transform.RequestBodyTransform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
// @Component
@RequiredArgsConstructor
public class RewriteGlobalFilter implements GlobalFilter, Ordered {

    private final ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;
    private final RequestBodyTransform requestBodyTransform;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("===================== pre filter =====================");

        return modifyRequestBodyFilter
                .apply(new ModifyRequestBodyGatewayFilterFactory.Config()
                        .setRewriteFunction(String.class, String.class, requestBodyTransform))
                .filter(exchange, chain)
                .then(Mono.fromRunnable(() -> {
                    log.info("===================== post filter =====================");
                }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}