package com.kt.kol.gateway.itg.strategy;

import org.springframework.http.HttpHeaders;

import com.kt.kol.common.model.SvcRequestInfoDTO;

/**
 * Endpoint 결정 전략 인터페이스
 * 도메인별 라우팅 로직의 확장성과 테스트 용이성 제공
 */
public interface EndpointStrategy {
    
    /**
     * 이 전략이 처리할 수 있는 도메인인지 확인
     */
    boolean supports(String appName);
    
    /**
     * 서비스 정보와 헤더를 기반으로 엔드포인트 결정
     */
    String determineEndpoint(SvcRequestInfoDTO svcRequestInfo, HttpHeaders headers);
    
    /**
     * 전략의 우선순위 (낮을수록 우선)
     */
    default int getPriority() {
        return 100;
    }
}