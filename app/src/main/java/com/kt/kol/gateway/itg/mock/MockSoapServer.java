package com.kt.kol.gateway.itg.mock;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * Mock SOAP 서버 - 실제 외부 서버 대신 Mock 응답 제공
 */
@Slf4j
@Component
public class MockSoapServer {

    private MockWebServer mockWebServer;
    private String baseUrl;

    @PostConstruct
    public void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        
        // SOAP 성공 응답 미리 설정
        setupDefaultResponses();
        
        // 서버 시작
        mockWebServer.start();
        baseUrl = mockWebServer.url("").toString();
        
        log.info("🚀 Mock SOAP Server started at: {}", baseUrl);
    }

    @PreDestroy
    public void stopServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
            log.info("🛑 Mock SOAP Server stopped");
        }
    }

    /**
     * 기본 SOAP 응답들을 설정
     */
    private void setupDefaultResponses() {
        // ORD 주문 처리 성공 응답
        mockWebServer.enqueue(createOrderSuccessResponse());
        
        // CRM 고객 조회 성공 응답
        mockWebServer.enqueue(createCustomerSuccessResponse());
        
        // 기본 성공 응답 (반복용)
        for (int i = 0; i < 100; i++) {
            mockWebServer.enqueue(createOrderSuccessResponse());
        }
    }

    /**
     * 주문 처리 성공 SOAP 응답
     */
    private MockResponse createOrderSuccessResponse() {
        String soapResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <orderResponse>
                        <responseType>I</responseType>
                        <orderId>ORD-20250917-001</orderId>
                        <status>COMPLETED</status>
                        <message>주문 처리 완료</message>
                        <customerName>김테스트</customerName>
                        <productCode>PROD-001</productCode>
                        <quantity>5</quantity>
                        <totalAmount>50000</totalAmount>
                        <orderDate>2025-09-17T15:30:00</orderDate>
                    </orderResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/xml; charset=utf-8")
                .setHeader("SOAPAction", "processOrderResponse")
                .setBody(soapResponse);
    }

    /**
     * 고객 조회 성공 SOAP 응답
     */
    private MockResponse createCustomerSuccessResponse() {
        String soapResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <customerResponse>
                        <responseType>I</responseType>
                        <customerId>CUST-001</customerId>
                        <customerName>홍길동</customerName>
                        <customerGrade>VIP</customerGrade>
                        <message>고객 정보 조회 완료</message>
                        <phoneNumber>010-1234-5678</phoneNumber>
                        <email>hong@example.com</email>
                        <address>서울시 강남구</address>
                    </customerResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/xml; charset=utf-8")
                .setHeader("SOAPAction", "getCustomerInfoResponse")
                .setBody(soapResponse);
    }

    /**
     * 비즈니스 에러 응답 추가
     */
    public void enqueueBusinessErrorResponse() {
        String errorResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <orderResponse>
                        <responseType>E</responseType>
                        <errorCode>ORD-E001</errorCode>
                        <errorMessage>재고 부족으로 주문 처리 불가</errorMessage>
                        <orderId>ORD-ERROR-001</orderId>
                    </orderResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/xml; charset=utf-8")
                .setBody(errorResponse));
    }

    /**
     * Mock 서버 URL 반환
     */
    public String getUrl() {
        return baseUrl;
    }

    /**
     * 마지막 요청 정보 반환 (디버깅용)
     */
    public RecordedRequest getLastRequest() throws InterruptedException {
        return mockWebServer.takeRequest();
    }

    /**
     * 요청 수 반환
     */
    public int getRequestCount() {
        return mockWebServer.getRequestCount();
    }
}