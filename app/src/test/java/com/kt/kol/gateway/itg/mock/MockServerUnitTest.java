package com.kt.kol.gateway.itg.mock;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock 서버 독립 단위 테스트
 * Application Context 없이 Mock 서버만 테스트
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MockServerUnitTest {

    private MockServerManager mockServerManager;
    private MockSoapServer ordMockServer;
    private MockSoapServer crmMockServer;
    private MockSoapServer stubMockServer;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws IOException {
        mockServerManager = new MockServerManager();
        ordMockServer = mockServerManager.startOrdMockServer();
        crmMockServer = mockServerManager.startCrmMockServer();
        stubMockServer = mockServerManager.startStubMockServer();
        restTemplate = new RestTemplate();
        
        log.info("=== Mock Server Unit Test Setup ===");
        mockServerManager.printMockServerStatus();
    }

    @AfterEach
    void tearDown() {
        if (mockServerManager != null) {
            mockServerManager.stopAllMockServers();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Mock 서버 시작 및 URL 확인 테스트")
    void testMockServerStartup() {
        // Given & When: Mock 서버들이 시작됨 (@BeforeEach)
        
        // Then: Mock 서버 URL들이 제대로 설정되었는지 확인
        Map<String, String> urls = mockServerManager.getAllMockServerUrls();
        
        assertThat(urls).hasSize(3);
        assertThat(urls).containsKeys("ORD", "CRM", "STUB");
        
        urls.forEach((name, url) -> {
            assertThat(url).startsWith("http://localhost:");
            assertThat(url).containsPattern("http://localhost:\\d+");
            log.info("✅ {} Mock Server: {}", name, url);
        });
        
        log.info("Mock 서버 시작 및 URL 확인 테스트 완료");
    }

    @Test
    @Order(2)
    @DisplayName("ORD 서비스 Mock 서버 응답 테스트")
    void testOrdMockServerResponse() throws InterruptedException {
        // Given: ORD 성공 응답 설정
        ordMockServer.enqueueOrdSuccessResponse();
        
        String testPayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <testRequest>Mock Server Test</testRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"processOrder\"");
        
        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        // When: Mock 서버에 직접 요청
        ResponseEntity<String> response = restTemplate.exchange(
            ordMockServer.getUrl(),
            HttpMethod.POST,
            request,
            String.class
        );

        // Then: 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("responseType>I</responseType>");
        assertThat(response.getBody()).contains("SUCCESS");
        assertThat(response.getBody()).contains("ORD-20250912-001");
        assertThat(response.getBody()).contains("주문 처리 완료");
        
        // Mock 서버에서 요청 검증
        RecordedRequest recordedRequest = ordMockServer.getLastRequest();
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getHeader("Content-Type")).contains("text/xml");
        assertThat(recordedRequest.getHeader("SOAPAction")).contains("processOrder");
        assertThat(recordedRequest.getBody().readUtf8()).contains("Mock Server Test");
        
        log.info("✅ ORD Mock 서버 응답 테스트 완료");
    }

    @Test
    @Order(3)
    @DisplayName("CRM 서비스 Mock 서버 응답 테스트")
    void testCrmMockServerResponse() throws InterruptedException {
        // Given: CRM 성공 응답 설정
        crmMockServer.enqueueCrmSuccessResponse();
        
        String testPayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <customerRequest>
                        <customerId>TEST-CUSTOMER-001</customerId>
                    </customerRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"getCustomerInfo\"");
        
        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        // When: CRM Mock 서버에 직접 요청
        ResponseEntity<String> response = restTemplate.exchange(
            crmMockServer.getUrl(),
            HttpMethod.POST,
            request,
            String.class
        );

        // Then: 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("responseType>I</responseType>");
        assertThat(response.getBody()).contains("SUCCESS");
        assertThat(response.getBody()).contains("홍길동");
        assertThat(response.getBody()).contains("VIP");
        assertThat(response.getBody()).contains("고객 정보 조회 완료");
        
        // Mock 서버에서 요청 검증
        RecordedRequest recordedRequest = crmMockServer.getLastRequest();
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getHeader("SOAPAction")).contains("getCustomerInfo");
        assertThat(recordedRequest.getBody().readUtf8()).contains("TEST-CUSTOMER-001");
        
        log.info("✅ CRM Mock 서버 응답 테스트 완료");
    }

    @Test
    @Order(4)
    @DisplayName("비즈니스 에러 응답 테스트")
    void testBusinessErrorResponse() throws InterruptedException {
        // Given: 비즈니스 에러 응답 설정
        ordMockServer.enqueueBusinessErrorResponse();
        
        String testPayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <orderRequest>
                        <productCode>OUT-OF-STOCK-PRODUCT</productCode>
                        <quantity>100</quantity>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        
        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        // When: Mock 서버에 요청
        ResponseEntity<String> response = restTemplate.exchange(
            ordMockServer.getUrl(),
            HttpMethod.POST,
            request,
            String.class
        );

        // Then: 비즈니스 에러 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("responseType>E</responseType>");
        assertThat(response.getBody()).contains("ORD-E001");
        assertThat(response.getBody()).contains("재고 부족");
        assertThat(response.getBody()).contains("주문 처리 오류");
        
        log.info("✅ 비즈니스 에러 응답 테스트 완료");
    }

    @Test
    @Order(5)
    @DisplayName("시스템 에러 (500) 응답 테스트")
    void testSystemErrorResponse() {
        // Given: 시스템 에러 응답 설정
        ordMockServer.enqueueSystemErrorResponse();
        
        String testPayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <orderRequest>
                        <productCode>ERROR-PRODUCT</productCode>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        
        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        // When: Mock 서버에 요청
        ResponseEntity<String> response = restTemplate.exchange(
            ordMockServer.getUrl(),
            HttpMethod.POST,
            request,
            String.class
        );

        // Then: 시스템 에러 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Internal Server Error");
        
        log.info("✅ 시스템 에러 (500) 응답 테스트 완료");
    }

    @Test
    @Order(6)
    @DisplayName("느린 응답 및 타임아웃 테스트")
    void testSlowResponseAndTimeout() {
        // Given: 3초 지연 응답 설정
        ordMockServer.enqueueSlowResponse(3);
        
        String testPayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <orderRequest>
                        <productCode>SLOW-PRODUCT</productCode>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        
        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        // When: Mock 서버에 요청 (시간 측정)
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(
            ordMockServer.getUrl(),
            HttpMethod.POST,
            request,
            String.class
        );
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Then: 느린 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("responseType>I</responseType>");
        assertThat(response.getBody()).contains("3초 지연된 응답입니다");
        assertThat(responseTime).isGreaterThan(3000L); // 3초 이상 소요
        
        log.info("✅ 느린 응답 테스트 완료 - 응답 시간: {}ms", responseTime);
    }

    @Test
    @Order(7)
    @DisplayName("서킷브레이커용 연속 실패 응답 테스트")
    void testCircuitBreakerFailureScenario() throws InterruptedException {
        // Given: 5개의 연속 실패 응답 설정
        ordMockServer.enqueueMultipleFailures(5);
        
        String testPayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <orderRequest>
                        <productCode>CIRCUIT-BREAKER-TEST</productCode>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        
        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        // When: 5번의 연속 요청
        int failureCount = 0;
        for (int i = 1; i <= 5; i++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    ordMockServer.getUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
                );
                
                if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                    failureCount++;
                }
                
                log.info("Request {}: Status = {}", i, response.getStatusCode());
            } catch (Exception e) {
                failureCount++;
                log.info("Request {}: Exception = {}", i, e.getClass().getSimpleName());
            }
        }

        // Then: 모든 요청이 실패했는지 확인
        assertThat(failureCount).isEqualTo(5);
        assertThat(ordMockServer.getRequestCount()).isEqualTo(5);
        
        log.info("✅ 서킷브레이커용 연속 실패 응답 테스트 완료 - 실패 수: {}", failureCount);
    }

    @Test
    @Order(8)
    @DisplayName("Mock 서버 시나리오별 동작 테스트")
    void testMockServerScenarios() throws InterruptedException {
        // Given: MockServerManager를 통한 시나리오 설정
        
        // When & Then: 성공 시나리오
        mockServerManager.setupSuccessScenario();
        
        // ORD 서비스 검증
        String ordPayload = createTestSoapRequest("ORD-SUCCESS-TEST");
        HttpEntity<String> ordRequest = new HttpEntity<>(ordPayload, createHeaders());
        ResponseEntity<String> ordResponse = restTemplate.exchange(
            ordMockServer.getUrl(), HttpMethod.POST, ordRequest, String.class);
        
        assertThat(ordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ordResponse.getBody()).contains("responseType>I</responseType>");
        
        // CRM 서비스 검증
        String crmPayload = createTestSoapRequest("CRM-SUCCESS-TEST");
        HttpEntity<String> crmRequest = new HttpEntity<>(crmPayload, createHeaders());
        ResponseEntity<String> crmResponse = restTemplate.exchange(
            crmMockServer.getUrl(), HttpMethod.POST, crmRequest, String.class);
        
        assertThat(crmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(crmResponse.getBody()).contains("responseType>I</responseType>");
        
        // When & Then: 비즈니스 에러 시나리오
        mockServerManager.setupBusinessErrorScenario();
        
        ResponseEntity<String> errorResponse = restTemplate.exchange(
            ordMockServer.getUrl(), HttpMethod.POST, ordRequest, String.class);
        
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(errorResponse.getBody()).contains("responseType>E</responseType>");
        
        log.info("✅ Mock 서버 시나리오별 동작 테스트 완료");
    }

    private String createTestSoapRequest(String requestId) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>TEST001</cmpnCd>
                        <userId>testuser</userId>
                        <requestId>%s</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <testRequest>
                        <message>Mock Server 시나리오 테스트</message>
                    </testRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """, requestId);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"testAction\"");
        return headers;
    }
}