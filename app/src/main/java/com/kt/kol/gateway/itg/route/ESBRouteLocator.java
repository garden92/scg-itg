package com.kt.kol.gateway.itg.route;

import java.time.Duration;

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
import com.kt.kol.gateway.itg.properties.RouteProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ESBRouteLocator {

	private final RouteProperties routeProperties;
	private final SoapRequestHandler soapRequestHandler;

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("rest-soap-po-route", r -> r
						.path("/SoapDynamicGateway")
						.filters(f -> applyCommonFilters(f))
						.uri("no://op"))
				.route("rest-soap-esb-route", r -> r
						.path("/SoapGateway")
						.filters(f -> applyCommonFilters(f))
						.uri("no://op"))
				.build();
	}

	private UriSpec applyCommonFilters(GatewayFilterSpec fn) {
		return fn
				// .requestRateLimiter(
				// config ->
				// config.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver()))
				.filter((exchange, chain) -> soapRequestHandler.handleRequest(exchange))
				.retry(config -> config
						.setRetries(routeProperties.getRetries())
						.setBackoff(Duration.ofMillis(routeProperties.getBackoffDelay()),
								Duration.ofMillis(routeProperties.getMaxBackoffDelay()),
								(int) routeProperties.getBackoffFactor(),
								true));
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