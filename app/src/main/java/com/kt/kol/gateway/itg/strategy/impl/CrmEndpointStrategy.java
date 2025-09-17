package com.kt.kol.gateway.itg.strategy.impl;

import com.kt.kol.gateway.itg.properties.SoapServiceProperties;
import com.kt.kol.gateway.itg.strategy.EndpointStrategy;
import com.kt.kol.common.model.SvcRequestInfoDTO;
import com.kt.kol.common.constant.DomainConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * CRM 서비스용 엔드포인트 전략
 * PO (어플리케이션 대 어플리케이션) vs ESB 라우팅 결정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrmEndpointStrategy implements EndpointStrategy {

    private final SoapServiceProperties soapServiceProperies;

    @Override
    public boolean supports(String appName) {
        boolean result = DomainConstants.CRM_DOMAIN_GROUP.contains(appName);
        log.debug("CrmEndpointStrategy.supports('{}') = {} (CRM_DOMAIN_GROUP: '{}')", 
                 appName, result, DomainConstants.CRM_DOMAIN_GROUP);
        return result;
    }

    @Override
    public String determineEndpoint(SvcRequestInfoDTO svcRequestInfo, HttpHeaders headers) {
        if (!supports(svcRequestInfo.appName())) {
            log.debug("CrmEndpointStrategy.determineEndpoint: appName '{}' not supported", svcRequestInfo.appName());
            return null;
        }

        String serviceType = svcRequestInfo.fnName();
        log.debug("CrmEndpointStrategy.determineEndpoint: serviceType='{}', fnName='{}'", 
                 serviceType, svcRequestInfo.fnName());

        if ("service".equals(serviceType)) {
            String endpoint = soapServiceProperies.getCrmPoEndPoint();
            log.debug("CrmEndpointStrategy: Using PO endpoint: {}", endpoint);
            return endpoint;
        } else {
            String endpoint = soapServiceProperies.getCrmEsbEndPoint();
            log.debug("CrmEndpointStrategy: Using ESB endpoint: {}", endpoint);
            return endpoint;
        }
    }

    @Override
    public int getPriority() {
        return 3;
    }
}