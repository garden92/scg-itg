package com.kt.kol.gateway.itg.config;

import com.kt.kol.gateway.itg.mock.MockServerManager;
import com.kt.kol.gateway.itg.mock.MockSoapServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

/**
 * E2E 테스트를 위한 완전한 테스트 설정
 * 실제 Gateway와 Mock 서버들을 연결하여 진짜 E2E 테스트 수행
 */
@Slf4j
@TestConfiguration
public class E2ETestConfig {
    
    private static MockServerManager mockServerManager;
    private static MockSoapServer ordMockServer;
    private static MockSoapServer crmMockServer;
    private static MockSoapServer stubMockServer;
    
    /**
     * 테스트 시작 전 Mock 서버들 초기화
     */
    static {
        try {
            initializeMockServers();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize mock servers for E2E tests", e);
        }
    }
    
    /**
     * Mock 서버들을 동적으로 초기화
     */
    private static void initializeMockServers() throws IOException {
        mockServerManager = new MockServerManager();
        ordMockServer = mockServerManager.startOrdMockServer();
        crmMockServer = mockServerManager.startCrmMockServer();
        stubMockServer = mockServerManager.startStubMockServer();
        
        log.info("🚀 E2E Test Mock Servers initialized:");
        log.info("   - ORD Mock Server: {}", ordMockServer.getUrl());
        log.info("   - CRM Mock Server: {}", crmMockServer.getUrl());
        log.info("   - STUB Mock Server: {}", stubMockServer.getUrl());
    }
    
    /**
     * 동적 프로퍼티 설정 - Mock 서버 URL들을 실제 애플리케이션 설정에 주입
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (ordMockServer != null && crmMockServer != null && stubMockServer != null) {
            // SOAP 서비스 엔드포인트를 Mock 서버로 설정
            registry.add("soap.service.ord-po-end-point", () -> ordMockServer.getUrl() + "/SoapDynamicGateway");
            registry.add("soap.service.ord-esb-end-point", () -> ordMockServer.getUrl() + "/SoapGateway");
            registry.add("soap.service.crm-po-end-point", () -> crmMockServer.getUrl() + "/SoapDynamicGateway");
            registry.add("soap.service.crm-esb-end-point", () -> crmMockServer.getUrl() + "/SoapGateway");
            registry.add("soap.service.stub-end-point", () -> stubMockServer.getUrl() + "/soap-perf-stub");
            
            // WebClient 타임아웃 설정 (테스트용으로 짧게)
            registry.add("webclient.connection-timeout", () -> "3000");
            registry.add("webclient.read-timeout", () -> "10000");
            registry.add("webclient.write-timeout", () -> "5000");
            
            // Feature 플래그 설정
            registry.add("feature.use-optimized-handler", () -> "true");
            
            // 테스트용 환경 변수 설정
            registry.add("NODE_IP", () -> "localhost");
            registry.add("kubernetes.node.ip", () -> "localhost");
            
            // Circuit Breaker 테스트용 설정
            registry.add("resilience4j.circuitbreaker.configs.default.sliding-window-size", () -> "3");
            registry.add("resilience4j.circuitbreaker.configs.default.minimum-number-of-calls", () -> "2");
            registry.add("resilience4j.circuitbreaker.configs.default.failure-rate-threshold", () -> "50");
            registry.add("resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state", () -> "2s");
            
            log.info("✅ Dynamic properties configured with Mock Server URLs");
        }
    }
    
    /**
     * MockServerManager Bean 등록
     */
    @Bean
    @Primary
    public MockServerManager mockServerManager() {
        return mockServerManager;
    }
    
    /**
     * ORD Mock Server Bean 등록  
     */
    @Bean
    public MockSoapServer ordMockServer() {
        return ordMockServer;
    }
    
    /**
     * CRM Mock Server Bean 등록
     */
    @Bean
    public MockSoapServer crmMockServer() {
        return crmMockServer;
    }
    
    /**
     * STUB Mock Server Bean 등록
     */
    @Bean
    public MockSoapServer stubMockServer() {
        return stubMockServer;
    }
    
    /**
     * 테스트 종료 시 Mock 서버들 정리
     */
    public static void cleanup() {
        if (mockServerManager != null) {
            mockServerManager.stopAllMockServers();
            log.info("🧹 E2E Test Mock Servers cleaned up");
        }
    }
}