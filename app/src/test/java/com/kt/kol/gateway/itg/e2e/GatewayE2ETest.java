package com.kt.kol.gateway.itg.e2e;

import com.kt.kol.gateway.itg.Application;
import com.kt.kol.gateway.itg.config.E2ETestConfig;
import com.kt.kol.gateway.itg.mock.MockServerManager;
import com.kt.kol.gateway.itg.mock.MockSoapServer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway E2E 테스트
 * 실제 Gateway를 통해 Mock 서버들과의 통합 테스트를 수행
 */
@Slf4j
@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(E2ETestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayE2ETest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private MockServerManager mockServerManager;
    
    @Autowired
    private MockSoapServer ordMockServer;
    
    @Autowired
    private MockSoapServer crmMockServer;
    
    @Autowired
    private MockSoapServer stubMockServer;
    
    @BeforeEach
    void setUp() {
        log.info("🚀 Gateway E2E Test Setup - Gateway Port: {}", gatewayPort);
        log.info("📊 Mock Server Status:");
        log.info("   - ORD Mock Server: {}", ordMockServer.getUrl());
        log.info("   - CRM Mock Server: {}", crmMockServer.getUrl());
        log.info("   - STUB Mock Server: {}", stubMockServer.getUrl());
        
        mockServerManager.printMockServerStatus();
    }
    
    @AfterEach
    void tearDown() {
        // Mock 서버들은 E2ETestConfig에서 관리됨
        log.info("🧹 Test method completed");
    }
    
    @AfterAll
    static void cleanupAll() {
        // 모든 테스트 완료 후 정리
        E2ETestConfig.cleanup();
        log.info("🏁 All E2E tests completed and cleaned up");
    }

    @Test
    @Order(1)
    @DisplayName("주문 처리 성공 시나리오 E2E 테스트")
    void testOrderProcessingSuccess() throws InterruptedException {
        // Given: ORD 서비스 성공 응답 설정
        ordMockServer.enqueueOrdSuccessResponse();
        
        String soapRequest = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>TEST001</cmpnCd>
                        <userId>testuser</userId>
                        <requestId>REQ-001</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderRequest>
                        <productCode>PROD-001</productCode>
                        <quantity>5</quantity>
                        <customerName>김테스트</customerName>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"processOrder\"");
        
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When: Gateway를 통해 SOAP 요청 전송
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
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
        
        // Mock 서버에서 요청 검증
        RecordedRequest recordedRequest = ordMockServer.getLastRequest();
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getHeader("Content-Type")).contains("text/xml");
        assertThat(recordedRequest.getBody().readUtf8()).contains("orderRequest");
        
        log.info("주문 처리 성공 테스트 완료");
    }

    @Test
    @Order(2) 
    @DisplayName("고객 정보 조회 성공 시나리오 E2E 테스트")
    void testCustomerInfoSuccess() throws InterruptedException {
        // Given: CRM 서비스 성공 응답 설정
        crmMockServer.enqueueCrmSuccessResponse();
        
        String soapRequest = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>CRM001</cmpnCd>
                        <userId>crmuser</userId>
                        <requestId>REQ-CRM-001</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <customerRequest>
                        <customerId>CUST-001</customerId>
                    </customerRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"getCustomerInfo\"");
        
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + gatewayPort + "/soap-dynamic-gateway", 
            HttpMethod.POST,
            request,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("responseType>I</responseType>");
        assertThat(response.getBody()).contains("홍길동");
        assertThat(response.getBody()).contains("VIP");
        
        log.info("고객 정보 조회 성공 테스트 완료");
    }

    @Test
    @Order(3)
    @DisplayName("비즈니스 에러 응답 E2E 테스트")
    void testBusinessError() throws InterruptedException {
        // Given: ORD 서비스 비즈니스 에러 응답 설정
        ordMockServer.enqueueBusinessErrorResponse();
        
        String soapRequest = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>TEST001</cmpnCd>
                        <userId>testuser</userId>
                        <requestId>REQ-ERROR-001</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderRequest>
                        <productCode>PROD-001</productCode>
                        <quantity>100</quantity>
                        <customerName>김테스트</customerName>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"processOrder\"");
        
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
            HttpMethod.POST, 
            request,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("responseType>E</responseType>");
        assertThat(response.getBody()).contains("ORD-E001");
        assertThat(response.getBody()).contains("재고 부족");
        
        log.info("비즈니스 에러 응답 테스트 완료");
    }

    @Test
    @Order(4)
    @DisplayName("스텁 서비스 라우팅 테스트 (B 접두사)")
    void testStubServiceRouting() throws InterruptedException {
        // Given: 스텁 서비스 응답 설정
        stubMockServer.enqueueOrdSuccessResponse();
        
        String soapRequest = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>B001</cmpnCd>
                        <userId>stubuser</userId>
                        <requestId>REQ-STUB-001</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <testRequest>
                        <message>스텁 서비스 테스트</message>
                    </testRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
            HttpMethod.POST,
            request, 
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("responseType>I</responseType>");
        
        // 스텁 서버에서 요청을 받았는지 확인
        RecordedRequest recordedRequest = stubMockServer.getLastRequest();
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getBody().readUtf8()).contains("스텁 서비스 테스트");
        
        log.info("스텁 서비스 라우팅 테스트 완료");
    }

    @Test
    @Order(5)
    @DisplayName("시스템 에러 (500) E2E 테스트") 
    void testSystemError() {
        // Given: ORD 서비스 시스템 에러 응답 설정
        ordMockServer.enqueueSystemErrorResponse();
        
        String soapRequest = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <cmpnCd>TEST001</cmpnCd>
                        <userId>testuser</userId>
                        <requestId>REQ-SYS-ERROR-001</requestId>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderRequest>
                        <productCode>ERROR-PROD</productCode>
                        <quantity>1</quantity>
                    </orderRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "\"processOrder\"");
        
        HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + gatewayPort + "/soap-dynamic-gateway",
            HttpMethod.POST,
            request,
            String.class
        );

        // Then: Gateway에서 에러 응답을 처리하는지 확인
        assertThat(response.getStatusCode()).isIn(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
        
        log.info("시스템 에러 (500) 테스트 완료");
    }

    @Test
    @Order(6)
    @DisplayName("Multiple Mock Servers URLs 확인")
    void testMockServerUrls() {
        // When
        Map<String, String> urls = mockServerManager.getAllMockServerUrls();
        
        // Then
        assertThat(urls).hasSize(3);
        assertThat(urls).containsKeys("ORD", "CRM", "STUB");
        
        urls.forEach((name, url) -> {
            assertThat(url).startsWith("http://localhost:");
            log.info("Mock Server {}: {}", name, url);
        });
        
        log.info("Mock Servers URL 확인 완료");
    }
}