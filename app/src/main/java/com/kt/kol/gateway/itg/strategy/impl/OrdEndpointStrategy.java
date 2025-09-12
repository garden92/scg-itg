package com.kt.kol.gateway.itg.strategy.impl;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.kt.kol.common.model.SvcRequestInfoDTO;
import com.kt.kol.common.util.DomainConstants;
import com.kt.kol.gateway.itg.properties.SoapServiceProperies;
import com.kt.kol.gateway.itg.strategy.EndpointStrategy;

import lombok.RequiredArgsConstructor;

/**
 * ORD(주문) 도메인 엔드포인트 전략
 * 주문 관련 서비스의 PO/ESB 엔드포인트 결정
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class OrdEndpointStrategy implements EndpointStrategy {
    
    private final SoapServiceProperies soapServiceProperies;
    
    @Override
    public boolean supports(String appName) {
        return DomainConstants.ORD_DOMAIN_GROUP.contains(appName);
    }
    
    @Override
    public String determineEndpoint(SvcRequestInfoDTO svcRequestInfo, HttpHeaders headers) {
        if (!supports(svcRequestInfo.appName())) {
            return null;
        }
        
        String serviceType = svcRequestInfo.fnName();
        
        if ("service".equals(serviceType)) {
            return soapServiceProperies.getOrdPoEndPoint();
        } else {
            return soapServiceProperies.getOrdEsbEndPoint();
        }
    }
    
    @Override
    public int getPriority() {
        return 3;
    }
}