package com.kt.kol.gateway.itg.e2e;

import com.kt.kol.gateway.itg.mock.MockServerManager;
import com.kt.kol.gateway.itg.mock.MockSoapServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load Test E2E 테스트
 * 성능 및 부하 테스트를 위한 E2E 테스트
 * @author Generated
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("성능 테스트는 필요시에만 실행") // 기본적으로 비활성화
class LoadTestE2E {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private MockServerManager mockServerManager;
    private MockSoapServer ordMockServer;
    private MockSoapServer crmMockServer;
    private MockSoapServer stubMockServer;

    @BeforeEach
    void setUp() throws IOException {
        mockServerManager = new MockServerManager();
        ordMockServer = mockServerManager.startOrdMockServer();
        crmMockServer = mockServerManager.startCrmMockServer();
        stubMockServer = mockServerManager.startStubMockServer();
        
        log.info("Load Test - Gateway started on port: {}", gatewayPort);
    }

    @AfterEach
    void tearDown() {
        if (mockServerManager != null) {
            mockServerManager.stopAllMockServers();
        }
    }

    @Test
    @Order(1)
    @DisplayName("기본 부하 테스트 - 100 동시 요청")
    void testBasicLoad() throws InterruptedException {
        // Given: 충분한 성공 응답 준비
        for (int i = 0; i < 100; i++) {
            ordMockServer.enqueueOrdSuccessResponse();
        }

        int totalRequests = 100;
        int concurrency = 10;
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        // When: 동시 부하 테스트 실행
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<Void>[] futures = IntStream.range(0, totalRequests)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                long requestStart = System.currentTimeMillis();
                
                try {
                    String soapRequest = createLoadTestSoapRequest("LOAD-" + i);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_XML);
                    HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
                        HttpMethod.POST,
                        request,
                        String.class
                    );

                    long requestEnd = System.currentTimeMillis();
                    totalResponseTime.addAndGet(requestEnd - requestStart);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    long requestEnd = System.currentTimeMillis();
                    totalResponseTime.addAndGet(requestEnd - requestStart);
                    errorCount.incrementAndGet();
                    log.warn("Load test request failed: {}", e.getMessage());
                }
            }, executor))
            .toArray(CompletableFuture[]::new);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        executor.shutdown();

        // Then: 성능 지표 분석
        double throughput = (double) totalRequests / (totalTime / 1000.0);
        double averageResponseTime = (double) totalResponseTime.get() / totalRequests;
        double successRate = (double) successCount.get() / totalRequests * 100;

        log.info("=== Load Test Results ===");
        log.info("Total Requests: {}", totalRequests);
        log.info("Successful Requests: {}", successCount.get());
        log.info("Failed Requests: {}", errorCount.get());
        log.info("Success Rate: {:.2f}%", successRate);
        log.info("Total Time: {}ms", totalTime);
        log.info("Throughput: {:.2f} req/s", throughput);
        log.info("Average Response Time: {:.2f}ms", averageResponseTime);
        log.info("========================");

        // 성능 기준 검증
        assertThat(successRate).isGreaterThan(95.0); // 95% 이상 성공률
        assertThat(averageResponseTime).isLessThan(1000.0); // 평균 응답시간 1초 미만
        assertThat(throughput).isGreaterThan(50.0); // 초당 50건 이상 처리
    }

    @Test
    @Order(2)
    @DisplayName("스파이크 테스트 - 순간 대량 요청")
    void testSpikeLoad() {
        // Given: 대량 요청을 위한 응답 준비
        for (int i = 0; i < 500; i++) {
            ordMockServer.enqueueOrdSuccessResponse();
        }

        int spikeRequests = 500;
        int concurrency = 50;
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // When: 순간적으로 대량 요청 발생
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        
        CompletableFuture<Void>[] futures = IntStream.range(0, spikeRequests)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    String soapRequest = createLoadTestSoapRequest("SPIKE-" + i);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_XML);
                    HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
                        HttpMethod.POST,
                        request,
                        String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }, executor))
            .toArray(CompletableFuture[]::new);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.join();
        
        long endTime = System.currentTimeMillis();
        long spikeTime = endTime - startTime;
        
        executor.shutdown();

        // Then: 스파이크 처리 성능 확인
        double spikeSuccessRate = (double) successCount.get() / spikeRequests * 100;
        double spikeThroughput = (double) spikeRequests / (spikeTime / 1000.0);

        log.info("=== Spike Test Results ===");
        log.info("Spike Requests: {}", spikeRequests);
        log.info("Successful: {}", successCount.get());
        log.info("Failed: {}", errorCount.get());
        log.info("Success Rate: {:.2f}%", spikeSuccessRate);
        log.info("Spike Time: {}ms", spikeTime);
        log.info("Spike Throughput: {:.2f} req/s", spikeThroughput);
        log.info("========================");

        // 스파이크 상황에서도 최소 성능 보장
        assertThat(spikeSuccessRate).isGreaterThan(80.0); // 80% 이상 성공률
        assertThat(spikeTime).isLessThan(30000); // 30초 내 완료
    }

    @Test 
    @Order(3)
    @DisplayName("지속 부하 테스트 - 10분간 일정 부하")
    void testSustainedLoad() throws InterruptedException {
        // Given: 지속적인 요청을 위한 대량 응답 준비
        for (int i = 0; i < 1000; i++) {
            ordMockServer.enqueueOrdSuccessResponse();
        }

        int duration = 60; // 1분 (테스트 시간 단축)
        int requestsPerSecond = 10;
        
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        // When: 지속적인 부하 테스트
        long testEndTime = System.currentTimeMillis() + (duration * 1000L);
        
        while (System.currentTimeMillis() < testEndTime) {
            long secondStart = System.currentTimeMillis();
            
            // 초당 요청 수만큼 요청 전송
            for (int i = 0; i < requestsPerSecond; i++) {
                int requestId = totalRequests.incrementAndGet();
                
                CompletableFuture.runAsync(() -> {
                    try {
                        String soapRequest = createLoadTestSoapRequest("SUSTAINED-" + requestId);
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.TEXT_XML);
                        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                            "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
                            HttpMethod.POST,
                            request,
                            String.class
                        );

                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }, executor);
            }
            
            // 초당 간격 조절
            long secondElapsed = System.currentTimeMillis() - secondStart;
            if (secondElapsed < 1000) {
                Thread.sleep(1000 - secondElapsed);
            }
        }
        
        // 모든 요청 완료 대기
        Thread.sleep(5000);
        executor.shutdown();

        // Then: 지속 부하 테스트 결과 분석
        double sustainedSuccessRate = (double) successCount.get() / totalRequests.get() * 100;

        log.info("=== Sustained Load Test Results ===");
        log.info("Test Duration: {} seconds", duration);
        log.info("Target RPS: {}", requestsPerSecond);
        log.info("Total Requests: {}", totalRequests.get());
        log.info("Successful: {}", successCount.get());
        log.info("Failed: {}", errorCount.get());
        log.info("Success Rate: {:.2f}%", sustainedSuccessRate);
        log.info("Actual RPS: {:.2f}", (double) totalRequests.get() / duration);
        log.info("================================");

        // 지속 부하에서의 성능 기준
        assertThat(sustainedSuccessRate).isGreaterThan(90.0); // 90% 이상 성공률 유지
        assertThat(totalRequests.get()).isGreaterThan((int)(duration * requestsPerSecond * 0.8)); // 목표 RPS의 80% 이상
    }

    private String createLoadTestSoapRequest(String requestId) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>LOAD001</cmpnCd>
                        <userId>loaduser</userId>
                        <requestId>%s</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderRequest>
                        <productCode>LOAD-PROD-001</productCode>
                        <quantity>1</quantity>
                        <customerName>로드테스트사용자</customerName>
                        <testType>LOAD_TEST</testType>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """, requestId);
    }
}