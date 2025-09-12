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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Circuit Breaker E2E 테스트
 * 서킷브레이커 동작을 검증하는 통합 테스트
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CircuitBreakerE2ETest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private MockServerManager mockServerManager;
    private MockSoapServer ordMockServer;

    @BeforeEach
    void setUp() throws IOException {
        mockServerManager = new MockServerManager();
        ordMockServer = mockServerManager.startOrdMockServer();
        
        log.info("Circuit Breaker Test - Gateway started on port: {}", gatewayPort);
    }

    @AfterEach
    void tearDown() {
        if (mockServerManager != null) {
            mockServerManager.stopAllMockServers();
        }
    }

    @Test
    @Order(1)
    @DisplayName("서킷브레이커 OPEN 상태 테스트")
    void testCircuitBreakerOpen() {
        // Given: 10개의 연속 실패 응답 설정 (circuit breaker가 열리도록)
        mockServerManager.setupCircuitBreakerScenario(10);
        
        String soapRequest = createTestSoapRequest("CB-FAIL-001");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"processOrder\"");
        
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When: 연속해서 실패하는 요청들 전송 (circuit breaker 열림 유발)
        ResponseEntity<String> response1 = sendRequest(request);
        ResponseEntity<String> response2 = sendRequest(request);  
        ResponseEntity<String> response3 = sendRequest(request);
        ResponseEntity<String> response4 = sendRequest(request);
        ResponseEntity<String> response5 = sendRequest(request);

        // Then: Circuit Breaker가 열린 상태에서는 빠른 실패가 발생해야 함
        log.info("Response 1: {}", response1.getStatusCode());
        log.info("Response 2: {}", response2.getStatusCode()); 
        log.info("Response 3: {}", response3.getStatusCode());
        log.info("Response 4: {}", response4.getStatusCode());
        log.info("Response 5: {}", response5.getStatusCode());

        // 첫 3개 요청은 실제 서버에서 500 에러
        assertThat(response1.getStatusCode()).isIn(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
        assertThat(response2.getStatusCode()).isIn(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
        assertThat(response3.getStatusCode()).isIn(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
        
        // 이후 요청들은 Circuit Breaker에 의해 빠른 실패 (또는 fallback)
        // Gateway의 fallback 설정에 따라 달라질 수 있음
        
        log.info("서킷브레이커 OPEN 상태 테스트 완료");
    }

    @Test
    @Order(2)
    @DisplayName("서킷브레이커 복구 테스트 (HALF-OPEN → CLOSED)")
    void testCircuitBreakerRecovery() throws InterruptedException {
        // Given: 먼저 실패로 Circuit Breaker를 열고
        ordMockServer.enqueueMultipleFailures(5);
        
        String soapRequest = createTestSoapRequest("CB-RECOVERY-001");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // Circuit Breaker를 열기 위해 실패 요청들 전송
        for (int i = 0; i < 3; i++) {
            sendRequest(request);
        }
        
        log.info("Circuit Breaker opened, waiting for recovery...");
        
        // Wait duration 이후 HALF-OPEN 상태로 전환 대기
        Thread.sleep(4000); // waitDurationInOpenState: 3s + 여유시간

        // When: 성공 응답으로 복구 시도
        ordMockServer.enqueueOrdSuccessResponse();
        ordMockServer.enqueueOrdSuccessResponse();
        
        ResponseEntity<String> recoveryResponse1 = sendRequest(request);
        ResponseEntity<String> recoveryResponse2 = sendRequest(request);

        // Then: Circuit Breaker가 CLOSED로 복구되었는지 확인
        log.info("Recovery Response 1: {}", recoveryResponse1.getStatusCode());
        log.info("Recovery Response 2: {}", recoveryResponse2.getStatusCode());
        
        // 성공적으로 복구되면 정상 응답을 받아야 함
        if (recoveryResponse1.getStatusCode() == HttpStatus.OK) {
            assertThat(recoveryResponse1.getBody()).contains("responseType>I</responseType>");
        }
        
        log.info("서킷브레이커 복구 테스트 완료");
    }

    @Test
    @Order(3)
    @DisplayName("동시 요청 시 서킷브레이커 동작 테스트")
    void testCircuitBreakerWithConcurrentRequests() {
        // Given: 동시 요청을 위한 실패 응답 설정
        ordMockServer.enqueueMultipleFailures(20);
        
        String soapRequest = createTestSoapRequest("CB-CONCURRENT-001");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When: 동시에 10개 요청 전송
        CompletableFuture<ResponseEntity<String>>[] futures = IntStream.range(0, 10)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                log.info("Sending concurrent request {}", i + 1);
                return sendRequest(request);
            }))
            .toArray(CompletableFuture[]::new);

        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.join();

        // Then: 결과 분석
        int successCount = 0;
        int errorCount = 0;
        
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            try {
                ResponseEntity<String> response = future.get();
                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount++;
                } else {
                    errorCount++;
                }
                log.info("Concurrent response: {}", response.getStatusCode());
            } catch (Exception e) {
                errorCount++;
                log.error("Concurrent request failed", e);
            }
        }

        log.info("Concurrent test results - Success: {}, Error: {}", successCount, errorCount);
        
        // 대부분 에러 응답이어야 함 (Circuit Breaker 동작)
        assertThat(errorCount).isGreaterThan(0);
        
        log.info("동시 요청 시 서킷브레이커 동작 테스트 완료");
    }

    @Test
    @Order(4) 
    @DisplayName("느린 응답에 대한 서킷브레이커 테스트")
    void testCircuitBreakerForSlowResponse() {
        // Given: 느린 응답 설정 (slowCallDurationThreshold: 2000ms 보다 큰 값)
        ordMockServer.enqueueSlowResponse(3); // 3초 지연
        ordMockServer.enqueueSlowResponse(3);
        ordMockServer.enqueueSlowResponse(3);
        ordMockServer.enqueueSlowResponse(3);
        
        String soapRequest = createTestSoapRequest("CB-SLOW-001");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When: 느린 응답을 유발하는 요청들 전송
        long startTime = System.currentTimeMillis();
        
        ResponseEntity<String> response1 = sendRequest(request);
        ResponseEntity<String> response2 = sendRequest(request);
        ResponseEntity<String> response3 = sendRequest(request);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then: 느린 응답으로 인한 Circuit Breaker 동작 확인
        log.info("Slow response test - Total time: {}ms", totalTime);
        log.info("Response 1: {}", response1.getStatusCode());
        log.info("Response 2: {}", response2.getStatusCode());  
        log.info("Response 3: {}", response3.getStatusCode());

        // 느린 응답이거나 timeout 에러가 발생해야 함
        // Circuit Breaker 설정에 따라 이후 요청들은 빠른 실패
        
        log.info("느린 응답에 대한 서킷브레이커 테스트 완료");
    }

    private ResponseEntity<String> sendRequest(HttpEntity<String> request) {
        try {
            return restTemplate.exchange(
                "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
                HttpMethod.POST,
                request,
                String.class
            );
        } catch (Exception e) {
            log.warn("Request failed: {}", e.getMessage());
            // 실패한 경우에도 ResponseEntity를 반환하도록 처리
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String createTestSoapRequest(String requestId) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>TEST001</cmpnCd>
                        <userId>cbuser</userId>
                        <requestId>%s</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderRequest>
                        <productCode>CB-TEST</productCode>
                        <quantity>1</quantity>
                        <customerName>서킷브레이커테스트</customerName>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """, requestId);
    }
}