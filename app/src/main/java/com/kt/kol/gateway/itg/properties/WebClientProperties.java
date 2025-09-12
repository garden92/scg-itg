package com.kt.kol.gateway.itg.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {
    private long timeout = 80000;
    private long readTimeout = 80000;
    private long writeTimeout = 80000;
    private int retries = 3;
    private long backoffDelay = 1000;
    private long maxBackoffDelay = 5000;
    private double backoffFactor = 2.0;
}
