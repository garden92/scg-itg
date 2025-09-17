package com.kt.kol.gateway.itg.strategy;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.kt.kol.common.model.SvcRequestInfoDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

/**
 * 엔드포인트 전략 해결자
 * 우선순위에 따라 적절한 전략을 선택하여 엔드포인트 결정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EndpointStrategyResolver {

    private final List<EndpointStrategy> strategies;

    @PostConstruct
    public void init() {
        log.info("EndpointStrategyResolver initialized with {} strategies", strategies.size());
        strategies.forEach(strategy -> 
            log.info("  - {}: priority {}", strategy.getClass().getSimpleName(), strategy.getPriority()));
    }

    /**
     * 요청 정보에 따라 적절한 엔드포인트를 결정
     * 우선순위 순서대로 전략을 시도하여 첫 번째로 지원하는 전략의 엔드포인트 반환
     */
    public String resolveEndpoint(SvcRequestInfoDTO svcRequestInfo, HttpHeaders headers) {
        log.debug("Resolving endpoint for appName: {}, fnName: {}", 
                 svcRequestInfo.appName(), svcRequestInfo.fnName());
        log.debug("Available strategies: {}", strategies.size());
        
        for (EndpointStrategy strategy : strategies) {
            log.debug("Trying strategy: {} (priority: {})", 
                     strategy.getClass().getSimpleName(), strategy.getPriority());
            
            if (strategy.supports(svcRequestInfo.appName())) {
                String endpoint = strategy.determineEndpoint(svcRequestInfo, headers);
                if (endpoint != null) {
                    log.debug("Strategy {} returned endpoint: {}", 
                             strategy.getClass().getSimpleName(), endpoint);
                    return endpoint;
                }
            } else {
                log.debug("Strategy {} does not support appName: {}", 
                         strategy.getClass().getSimpleName(), svcRequestInfo.appName());
            }
        }

        log.error("No suitable endpoint strategy found for appName: {}, fnName: {}", 
                 svcRequestInfo.appName(), svcRequestInfo.fnName());
        throw new IllegalArgumentException(
                String.format("No suitable endpoint strategy found for appName: %s, fnName: %s",
                        svcRequestInfo.appName(), svcRequestInfo.fnName()));
    }
}