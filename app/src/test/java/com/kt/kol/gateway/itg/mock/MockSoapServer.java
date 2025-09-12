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
     * 성공 응답 Mock 설정
     */
    public void enqueueSuccessResponse() {
        String successResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>I</responseType>
                        <responseCode>SUCCESS</responseCode>
                        <responseTitle>Success</responseTitle>
                        <responseBasc>Operation completed successfully</responseBasc>
                        <responseDtal>Mock response</responseDtal>
                        <responseSystem>MOCK</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <service_response>
                        <result>
                            <status>SUCCESS</status>
                            <message>Mock data processed</message>
                            <data>
                                <id>12345</id>
                                <name>Test User</name>
                            </data>
                        </result>
                    </service_response>
                </soapenv:Body>
            </soapenv:Envelope>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setBody(successResponse));
    }
    
    /**
     * 비즈니스 에러 응답 Mock 설정
     */
    public void enqueueBusinessErrorResponse() {
        String errorResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>E</responseType>
                        <responseCode>BUSINESS_ERROR</responseCode>
                        <responseTitle>Business Error</responseTitle>
                        <responseBasc>Invalid request data</responseBasc>
                        <responseDtal>Required field missing</responseDtal>
                        <responseSystem>MOCK</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <service_response>
                        <error>
                            <code>E001</code>
                            <message>Validation failed</message>
                        </error>
                    </service_response>
                </soapenv:Body>
            </soapenv:Envelope>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setBody(errorResponse));
    }
    
    /**
     * 시스템 에러 응답 Mock 설정
     */
    public void enqueueSystemErrorResponse() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setBody("Internal Server Error"));
    }
    
    /**
     * 느린 응답 Mock 설정 (타임아웃 테스트용)
     */
    public void enqueueSlowResponse(int delaySeconds) {
        String successResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>I</responseType>
                        <responseCode>SUCCESS</responseCode>
                        <responseTitle>Slow Response</responseTitle>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <service_response>
                        <result>Delayed response</result>
                    </service_response>
                </soapenv:Body>
            </soapenv:Envelope>
            """;
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/xml; charset=UTF-8")
            .setBody(successResponse)
            .setBodyDelay(delaySeconds, TimeUnit.SECONDS));
    }
    
    /**
     * 요청 검증용 - 마지막 요청 조회
     */
    public RecordedRequest getLastRequest() throws InterruptedException {
        return mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    }
    
    /**
     * 요청 수 조회
     */
    public int getRequestCount() {
        return mockWebServer.getRequestCount();
    }
}