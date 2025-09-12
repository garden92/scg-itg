package com.kt.kol.gateway.itg.strategy;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.kt.kol.common.model.SvcRequestInfoDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint 전략 해결자
 * 우선순위에 따라 적절한 전략을 선택하여 엔드포인트 결정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EndpointStrategyResolver {
    
    private final List<EndpointStrategy> strategies;
    
    /**
     * 서비스 정보와 헤더를 기반으로 엔드포인트 결정
     * 
     * @param svcRequestInfo 서비스 요청 정보
     * @param headers HTTP 헤더
     * @return 결정된 엔드포인트 URL
     * @throws IllegalArgumentException 적절한 엔드포인트를 찾을 수 없는 경우
     */
    public String resolveEndpoint(SvcRequestInfoDTO svcRequestInfo, HttpHeaders headers) {
        log.debug("Resolving endpoint for appName: {}, fnName: {}", 
                 svcRequestInfo.appName(), svcRequestInfo.fnName());
        
        // 우선순위에 따라 전략 실행
        Optional<String> endpoint = strategies.stream()
                .sorted((s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()))
                .map(strategy -> {
                    try {
                        String result = strategy.determineEndpoint(svcRequestInfo, headers);
                        if (result != null) {
                            log.debug("Strategy {} determined endpoint: {}", 
                                     strategy.getClass().getSimpleName(), result);
                        }
                        return result;
                    } catch (Exception e) {
                        log.warn("Strategy {} failed with error: {}", 
                                strategy.getClass().getSimpleName(), e.getMessage());
                        return null;
                    }
                })
                .filter(result -> result != null)
                .findFirst();
        
        if (endpoint.isPresent()) {
            log.info("Resolved endpoint: {} for appName: {}", endpoint.get(), svcRequestInfo.appName());
            return endpoint.get();
        }
        
        // 적절한 전략을 찾지 못한 경우
        String errorMsg = String.format("No suitable endpoint strategy found for appName: %s, fnName: %s", 
                                       svcRequestInfo.appName(), svcRequestInfo.fnName());
        log.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
    }
    
    /**
     * 등록된 전략 수 조회 (모니터링/디버깅용)
     */
    public int getStrategyCount() {
        return strategies.size();
    }
    
    /**
     * 전략 목록 조회 (모니터링/디버깅용)
     */
    public List<String> getStrategyNames() {
        return strategies.stream()
                .map(strategy -> strategy.getClass().getSimpleName())
                .sorted()
                .toList();
    }
}