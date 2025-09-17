package com.kt.kol.gateway.itg.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "soap.service")
public class SoapServiceProperties {
    private String ordPoEndPoint;
    private String ordEsbEndPoint;
    private String crmPoEndPoint;
    private String crmEsbEndPoint;
    private String stubEndPoint; // 성능테스트 임시
    private long timeout = 80000;
}