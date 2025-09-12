package com.kt.kol.gateway.itg.route;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.kt.kol.gateway.itg.handler.SoapRequestHandler;

import com.kt.kol.common.constant.RouteConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ESBRouteLocator {

	private final SoapRequestHandler soapRequestHandler;

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(RouteConstants.REST_SOAP_PO_ROUTE, r -> r
						.path(RouteConstants.SOAP_DYNAMIC_GATEWAY_PATH)
						.filters(f -> applyCommonFilters(f))
						.uri(RouteConstants.NO_OP_URI))
				.route(RouteConstants.REST_SOAP_ESB_ROUTE, r -> r
						.path(RouteConstants.SOAP_GATEWAY_PATH)
						.filters(f -> applyCommonFilters(f))
						.uri(RouteConstants.NO_OP_URI))
				.build();
	}

	private UriSpec applyCommonFilters(GatewayFilterSpec fn) {
		return fn
				// .requestRateLimiter(
				// config ->
				// config.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver()))
				.filter((exchange, chain) -> soapRequestHandler.handleRequest(exchange));
	}

	// @Bean
	// RedisRateLimiter redisRateLimiter() {
	// return new RedisRateLimiter(1, 2);
	// Token Bucket 알고리즘 시반
	// replenishRate: 초당처리량
	// burstCapacity : 최대 버킷크기
	// 일시적으로 최대 2 request/second를 처리할 수 있으며, 평균적으로는 1 request/second를 처리하며, 이를 초과하는
	// 요청에 대해서는 HTTP 429 - Too Many Requets를 리턴
	// }

	// @Bean
	// public KeyResolver userKeyResolver() {
	// return exchange -> Mono.just(exchange.getRequest()
	// .getHeaders()
	// .getFirst("X-User-ID") != null ?
	// exchange.getRequest().getHeaders().getFirst("X-User-ID")
	// : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
	// }
}