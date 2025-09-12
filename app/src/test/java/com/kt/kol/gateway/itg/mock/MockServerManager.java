package com.kt.kol.gateway.itg.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock 서버들을 관리하는 유틸리티 클래스
 * E2E 테스트에서 여러 서비스를 동시에 Mock할 때 사용
 */
@Slf4j
@Component
public class MockServerManager {
    
    private final Map<String, MockSoapServer> mockServers = new HashMap<>();
    
    /**
     * ORD 서비스 Mock 서버 시작
     */
    public MockSoapServer startOrdMockServer() throws IOException {
        MockSoapServer ordServer = new MockSoapServer();
        ordServer.start();
        mockServers.put("ORD", ordServer);
        log.info("ORD Mock Server started at: {}", ordServer.getUrl());
        return ordServer;
    }
    
    /**
     * CRM 서비스 Mock 서버 시작
     */
    public MockSoapServer startCrmMockServer() throws IOException {
        MockSoapServer crmServer = new MockSoapServer();
        crmServer.start();
        mockServers.put("CRM", crmServer);
        log.info("CRM Mock Server started at: {}", crmServer.getUrl());
        return crmServer;
    }
    
    /**
     * 스텁 서비스 Mock 서버 시작
     */
    public MockSoapServer startStubMockServer() throws IOException {
        MockSoapServer stubServer = new MockSoapServer();
        stubServer.start();
        mockServers.put("STUB", stubServer);
        log.info("STUB Mock Server started at: {}", stubServer.getUrl());
        return stubServer;
    }
    
    /**
     * 특정 서비스의 Mock 서버 가져오기
     */
    public MockSoapServer getMockServer(String serviceName) {
        return mockServers.get(serviceName.toUpperCase());
    }
    
    /**
     * 모든 Mock 서버들의 URL 정보 반환
     */
    public Map<String, String> getAllMockServerUrls() {
        Map<String, String> urls = new HashMap<>();
        mockServers.forEach((name, server) -> {
            urls.put(name, server.getUrl());
        });
        return urls;
    }
    
    /**
     * 특정 Mock 서버 중지
     */
    public void stopMockServer(String serviceName) {
        MockSoapServer server = mockServers.get(serviceName.toUpperCase());
        if (server != null) {
            try {
                server.stop();
                mockServers.remove(serviceName.toUpperCase());
                log.info("{} Mock Server stopped", serviceName);
            } catch (IOException e) {
                log.error("Failed to stop {} Mock Server", serviceName, e);
            }
        }
    }
    
    /**
     * 모든 Mock 서버 중지
     */
    public void stopAllMockServers() {
        mockServers.forEach((name, server) -> {
            try {
                server.stop();
                log.info("{} Mock Server stopped", name);
            } catch (IOException e) {
                log.error("Failed to stop {} Mock Server", name, e);
            }
        });
        mockServers.clear();
        log.info("All Mock Servers stopped");
    }
    
    /**
     * 성공 시나리오 설정 - ORD와 CRM 모두 성공 응답
     */
    public void setupSuccessScenario() {
        MockSoapServer ordServer = mockServers.get("ORD");
        MockSoapServer crmServer = mockServers.get("CRM");
        
        if (ordServer != null) {
            ordServer.enqueueOrdSuccessResponse();
            log.info("ORD success response enqueued");
        }
        
        if (crmServer != null) {
            crmServer.enqueueCrmSuccessResponse();  
            log.info("CRM success response enqueued");
        }
    }
    
    /**
     * 에러 시나리오 설정 - 비즈니스 에러
     */
    public void setupBusinessErrorScenario() {
        MockSoapServer ordServer = mockServers.get("ORD");
        if (ordServer != null) {
            ordServer.enqueueBusinessErrorResponse();
            log.info("ORD business error response enqueued");
        }
    }
    
    /**
     * 타임아웃 시나리오 설정 - 느린 응답
     */
    public void setupTimeoutScenario(int delaySeconds) {
        MockSoapServer ordServer = mockServers.get("ORD");
        if (ordServer != null) {
            ordServer.enqueueSlowResponse(delaySeconds);
            log.info("ORD slow response ({} seconds) enqueued", delaySeconds);
        }
    }
    
    /**
     * 서킷브레이커 시나리오 설정 - 연속 실패
     */
    public void setupCircuitBreakerScenario(int failureCount) {
        MockSoapServer ordServer = mockServers.get("ORD");
        if (ordServer != null) {
            ordServer.enqueueMultipleFailures(failureCount);
            log.info("Circuit breaker scenario setup with {} failures", failureCount);
        }
    }
    
    /**
     * 현재 실행 중인 Mock 서버 상태 확인
     */
    public void printMockServerStatus() {
        log.info("=== Mock Server Status ===");
        if (mockServers.isEmpty()) {
            log.info("No Mock Servers running");
        } else {
            mockServers.forEach((name, server) -> {
                log.info("{}: {} (Requests: {})", name, server.getUrl(), server.getRequestCount());
            });
        }
        log.info("========================");
    }
}