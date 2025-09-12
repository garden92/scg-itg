package com.kt.kol.gateway.itg.mock;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * SOAP 서비스 Mock 서버
 * 성능 테스트 및 기능 검증용
 */
@Slf4j
public class MockSoapServer {
    
    private MockWebServer mockWebServer;
    private int port;
    
    public void start() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        port = mockWebServer.getPort();
        log.info("Mock SOAP Server started on port: {}", port);
    }
    
    public void stop() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
            log.info("Mock SOAP Server stopped");
        }
    }
    
    public String getUrl() {
        return String.format("http://localhost:%d", port);
    }
    
    /**
     * ORD 주문 성공 응답 Mock
     */
    public void enqueueOrdSuccessResponse() {
        String successResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>I</responseType>
                        <responseCode>SUCCESS</responseCode>
                        <responseTitle>주문 처리 완료</responseTitle>
                        <responseBasc>주문이 성공적으로 처리되었습니다.</responseBasc>
                        <responseDtal>Order ID: ORD-20250912-001</responseDtal>
                        <responseSystem>ORD</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderResponse>
                        <result>
                            <orderId>ORD-20250912-001</orderId>
                            <status>COMPLETED</status>
                            <amount>50000</amount>
                            <customerName>김테스트</customerName>
                            <orderDate>2025-09-12T10:30:00</orderDate>
                        </result>
                    </orderResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setHeader("SOAPAction", "\"processOrder\"")
            .setBody(successResponse));
    }
    
    /**
     * CRM 고객 정보 조회 성공 응답 Mock
     */
    public void enqueueCrmSuccessResponse() {
        String successResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>I</responseType>
                        <responseCode>SUCCESS</responseCode>
                        <responseTitle>고객 정보 조회 완료</responseTitle>
                        <responseBasc>고객 정보가 성공적으로 조회되었습니다.</responseBasc>
                        <responseDtal>Customer ID: CUST-001</responseDtal>
                        <responseSystem>CRM</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <customerResponse>
                        <result>
                            <customerId>CUST-001</customerId>
                            <name>홍길동</name>
                            <phone>010-1234-5678</phone>
                            <email>hong@example.com</email>
                            <grade>VIP</grade>
                            <joinDate>2023-01-15</joinDate>
                        </result>
                    </customerResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setHeader("SOAPAction", "\"getCustomerInfo\"")
            .setBody(successResponse));
    }
    
    /**
     * 비즈니스 에러 응답 Mock
     */
    public void enqueueBusinessErrorResponse() {
        String errorResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>E</responseType>
                        <responseCode>ORD-E001</responseCode>
                        <responseTitle>주문 처리 오류</responseTitle>
                        <responseBasc>재고 부족으로 주문을 처리할 수 없습니다.</responseBasc>
                        <responseDtal>상품 코드: PROD-001, 요청 수량: 100, 재고 수량: 50</responseDtal>
                        <responseSystem>ORD</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <orderResponse>
                        <error>
                            <code>ORD-E001</code>
                            <message>재고 부족</message>
                            <productCode>PROD-001</productCode>
                            <requestedQuantity>100</requestedQuantity>
                            <availableQuantity>50</availableQuantity>
                        </error>
                    </orderResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setHeader("SOAPAction", "\"processOrder\"")
            .setBody(errorResponse));
    }
    
    /**
     * 시스템 에러 응답 Mock (500 에러)
     */
    public void enqueueSystemErrorResponse() {
        String errorResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Fault xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <faultcode>Server</faultcode>
                <faultstring>Internal Server Error</faultstring>
                <detail>
                    <message>Database connection failed</message>
                    <errorCode>SYS-E500</errorCode>
                </detail>
            </soap:Fault>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setBody(errorResponse));
    }
    
    /**
     * 느린 응답 Mock 설정 (타임아웃 테스트용)
     */
    public void enqueueSlowResponse(int delaySeconds) {
        String slowResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>I</responseType>
                        <responseCode>SUCCESS</responseCode>
                        <responseTitle>느린 응답 테스트</responseTitle>
                        <responseBasc>%d초 지연된 응답입니다.</responseBasc>
                        <responseDtal>Delay Test Response</responseDtal>
                        <responseSystem>MOCK</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <slowResponse>
                        <result>지연된 응답이 완료되었습니다.</result>
                        <delaySeconds>%d</delaySeconds>
                    </slowResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(delaySeconds, delaySeconds);
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setBody(slowResponse)
            .setBodyDelay(delaySeconds, TimeUnit.SECONDS));
    }
    
    /**
     * 서킷브레이커 테스트용 - 연속 실패 응답
     */
    public void enqueueMultipleFailures(int count) {
        for (int i = 0; i < count; i++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/xml; charset=UTF-8")
                .setBody("Internal Server Error"));
        }
        log.info("Enqueued {} failure responses for circuit breaker test", count);
    }
    
    /**
     * 요청 검증용 - 마지막 요청 조회
     */
    public RecordedRequest getLastRequest() throws InterruptedException {
        return mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    }
    
    /**
     * 모든 요청 조회
     */
    public RecordedRequest takeRequest() throws InterruptedException {
        return mockWebServer.takeRequest();
    }
    
    /**
     * 요청 수 조회
     */
    public int getRequestCount() {
        return mockWebServer.getRequestCount();
    }
    
    /**
     * 대기 중인 요청 수 조회  
     */
    public int getRequestCountWaiting() {
        return mockWebServer.getRequestCount();
    }
}