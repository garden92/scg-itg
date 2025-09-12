package com.kt.kol.gateway.itg.infra;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelOption;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.kt.kol.gateway.itg.properties.WebClientProperties;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@EnableConfigurationProperties(WebClientProperties.class)
@Slf4j
public class WebClientConfig {
	private SslContext sslContext;

	@Bean
	public WebClient webClient(WebClientProperties webClientProperties) {
		try {
			sslContext = SslContextBuilder.forClient()
			        .trustManager(InsecureTrustManagerFactory.INSTANCE)
			        .build();
		} catch (SSLException e) {
			e.printStackTrace();
		}

		// SOAP 서비스 최적화된 커넥션 풀 설정
		ConnectionProvider connectionProvider = ConnectionProvider.builder("soap-client")
				.maxConnections(200)                                    // 최대 200개 연결
				.maxIdleTime(Duration.ofSeconds(20))                   // 20초 후 idle 연결 해제  
				.maxLifeTime(Duration.ofSeconds(60))                   // 60초 후 연결 재생성
				.pendingAcquireMaxCount(100)                           // 대기 큐 크기
				.evictInBackground(Duration.ofSeconds(120))            // 백그라운드 정리 주기
				.build();

		HttpClient httpClient = HttpClient.create(connectionProvider)
				.option(ChannelOption.SO_KEEPALIVE, true)              // TCP Keep-Alive 활성화
				.option(ChannelOption.TCP_NODELAY, true)               // Nagle 알고리즘 비활성화
				.secure(sslSpec -> sslSpec.sslContext(sslContext))
				.responseTimeout(Duration.ofMillis(webClientProperties.getTimeout()))
				.doOnConnected(conn -> conn
						.addHandlerLast(
								new ReadTimeoutHandler(
										webClientProperties.getReadTimeout(),
										TimeUnit.MILLISECONDS))
						.addHandlerLast(
								new WriteTimeoutHandler(
										webClientProperties.getWriteTimeout(),
										TimeUnit.MILLISECONDS)));

		return WebClient.builder()
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.defaultHeaders(headers -> headers.set(HttpHeaders.CONTENT_TYPE,
						MediaType.TEXT_XML_VALUE))
				.filter(ExchangeFilterFunction.ofRequestProcessor(
						clientRequest -> {
							log.info("Request: {} {}", clientRequest.method(),
									clientRequest.url());
							return Mono.just(clientRequest);
						}))
				.filter(ExchangeFilterFunction.ofResponseProcessor(
						clientResponse -> {
							log.info("Response: {} {}", clientResponse.statusCode(),
									clientResponse.headers().asHttpHeaders());
							return Mono.just(clientResponse);
						}))
				.build();
	}

}
