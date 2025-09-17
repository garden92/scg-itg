package com.kt.kol.gateway.itg.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {
    private long connectionTimeout = 5000;    // 연결 설정 타임아웃
    private long readTimeout = 95000;         // 응답 대기 타임아웃
    private long writeTimeout = 10000;        // 요청 전송 타임아웃
}
