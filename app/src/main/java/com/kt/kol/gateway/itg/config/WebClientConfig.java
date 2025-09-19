package com.kt.kol.gateway.itg.config;

import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.kt.kol.gateway.itg.properties.WebClientProperties;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@EnableConfigurationProperties(WebClientProperties.class)
@Slf4j
public class WebClientConfig {

	@Bean
	@Profile("dev")
	SslContext devSslContext() {
		try {
			return SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE)
					.build();
		} catch (SSLException e) {
			throw new IllegalStateException("Dev SSL context failed", e);
		}
	}

	@Bean
	@Profile("!dev")
	SslContext prodSslContext() throws SSLException {
		// 기본 JDK 신뢰 스토어 사용(필요시 커스텀 TrustManager)
		return SslContextBuilder.forClient().build();
	}

	@Bean
	public WebClient webClient(
			WebClientProperties props,
			SslContext sslContext) {

		ConnectionProvider provider = ConnectionProvider.builder("soap-client")
				.maxConnections(Math.min(100, Runtime.getRuntime().availableProcessors() * 10))
				.pendingAcquireMaxCount(100)
				.maxIdleTime(Duration.ofSeconds(30))
				.maxLifeTime(Duration.ofMinutes(5))
				.metrics(true)
				.evictInBackground(Duration.ofSeconds(60))
				.build();

		HttpClient httpClient = HttpClient.create(provider)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.getConnectionTimeout())
				.responseTimeout(Duration.ofMillis(props.getReadTimeout()))
				.secure(ssl -> ssl.sslContext(sslContext))
				.compress(true)
				.httpResponseDecoder(decoder -> decoder
						.maxInitialLineLength(8192)
						.maxHeaderSize(32 * 1024)
						.validateHeaders(true));

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.defaultHeaders(h -> {
					// 공통 헤더(추적용). Content-Type은 요청 빌드 시 넣기
					h.add("X-Correlation-Id", java.util.UUID.randomUUID().toString());
					h.set(HttpHeaders.ACCEPT, MediaType.TEXT_XML_VALUE);
				})
				.codecs(c -> {
					c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
					c.defaultCodecs().enableLoggingRequestDetails(false);
				})
				.filter(loggingFilter()) // 개선된 로깅 필터
				.filter(correlationIdPropagator()) // MDC/헤더 전파
				.build();
	}

	private ExchangeFilterFunction loggingFilter() {
		return ExchangeFilterFunction.ofRequestProcessor(req -> {
			if (log.isDebugEnabled()) {
				log.debug("[REQ] {} {} headers={}", req.method(), req.url(), req.headers());
			}
			return Mono.just(req);
		}).andThen(ExchangeFilterFunction.ofResponseProcessor(res -> {
			if (log.isDebugEnabled()) {
				log.debug("[RES] {} headers={}", res.statusCode(), res.headers().asHttpHeaders());
			}
			return Mono.just(res);
		}));
	}

	private ExchangeFilterFunction correlationIdPropagator() {
		return ExchangeFilterFunction.ofRequestProcessor(req -> {
			String cid = req.headers().getFirst("X-Correlation-Id");
			if (cid == null) {
				cid = java.util.UUID.randomUUID().toString();
				return Mono.just(ClientRequest.from(req)
						.header("X-Correlation-Id", cid)
						.build());
			}
			return Mono.just(req);
		});
	}
}
