package com.kt.kol.gateway.itg.config;

import com.kt.kol.gateway.itg.mock.MockSoapServer;
import com.kt.kol.gateway.itg.properties.SoapServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * Mock 프로파일에서 Mock 서버 URL을 SOAP 서비스 설정에 적용
 */
@Slf4j
@Configuration
@Profile("mock")
@RequiredArgsConstructor
public class MockServerConfiguration {

    private final MockSoapServer mockSoapServer;
    private final SoapServiceProperties soapServiceProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void configureMockUrls() {
        String mockUrl = mockSoapServer.getUrl();
        
        // Mock 서버 URL을 SOAP 서비스 속성에 설정
        soapServiceProperties.setOrdPoEndPoint(mockUrl + "SoapDynamicGateway");
        soapServiceProperties.setOrdEsbEndPoint(mockUrl + "SoapGateway");
        soapServiceProperties.setCrmPoEndPoint(mockUrl + "SoapDynamicGateway");
        soapServiceProperties.setCrmEsbEndPoint(mockUrl + "SoapGateway");
        soapServiceProperties.setStubEndPoint(mockUrl + "soap-perf-stub");

        log.info("🔧 Mock SOAP URLs configured:");
        log.info("   - ORD PO: {}", soapServiceProperties.getOrdPoEndPoint());
        log.info("   - ORD ESB: {}", soapServiceProperties.getOrdEsbEndPoint());
        log.info("   - CRM PO: {}", soapServiceProperties.getCrmPoEndPoint());
        log.info("   - CRM ESB: {}", soapServiceProperties.getCrmEsbEndPoint());
        log.info("   - STUB: {}", soapServiceProperties.getStubEndPoint());
    }
}