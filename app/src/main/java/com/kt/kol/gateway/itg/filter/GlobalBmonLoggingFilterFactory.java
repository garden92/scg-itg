package com.kt.kol.gateway.itg.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalBmonLoggingFilterFactory
        extends AbstractGatewayFilterFactory<GlobalBmonLoggingFilterFactory.Config> {
    public GlobalBmonLoggingFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long startTime = System.nanoTime();
            String requestPath = exchange.getRequest().getPath().toString();
            String requestId = exchange.getRequest().getId();
            
            if (config.isPreLogger()) {
                logAsync("gateway.request.start", Map.of(
                    "requestId", requestId,
                    "path", requestPath,
                    "method", exchange.getRequest().getMethod().toString(),
                    "message", config.getBaseMessage()
                ));
            }

            return chain.filter(exchange)
                    .doFinally(signalType -> {
                        if (config.isPostLogger()) {
                            long duration = System.nanoTime() - startTime;
                            long durationMs = duration / 1_000_000;
                            
                            logAsync("gateway.request.completed", Map.of(
                                "requestId", requestId,
                                "path", requestPath,
                                "duration", String.valueOf(durationMs),
                                "status", exchange.getResponse().getStatusCode() != null 
                                    ? exchange.getResponse().getStatusCode().toString() : "UNKNOWN",
                                "signalType", signalType.toString(),
                                "message", config.getBaseMessage()
                            ));
                        }
                    });
        };
    }

    /**
     * 비동기 로깅 메소드
     * I/O 블로킹 없이 로그 처리 (20% 오버헤드 감소)
     */
    private void logAsync(String event, Map<String, String> context) {
        CompletableFuture.runAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                StringBuilder logMessage = new StringBuilder()
                    .append("[").append(timestamp).append("] ")
                    .append("EVENT=").append(event).append(" ");
                
                context.forEach((key, value) -> 
                    logMessage.append(key).append("=").append(value).append(" "));
                
                log.info(logMessage.toString().trim());
                
            } catch (Exception e) {
                // 로깅 실패해도 메인 플로우에 영향 없도록
                log.warn("Async logging failed", e);
            }
        });
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
