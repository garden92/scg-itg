package com.kt.kol.gateway.itg.strategy.impl;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.kt.kol.common.constant.HeaderConstants;
import com.kt.kol.common.model.SvcRequestInfoDTO;
import com.kt.kol.gateway.itg.properties.SoapServiceProperties;
import com.kt.kol.gateway.itg.strategy.EndpointStrategy;

import lombok.RequiredArgsConstructor;

/**
 * 성능테스트용 Stub 엔드포인트 전략
 * 최고 우선순위로 B로 시작하는 cmpnCd 처리
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class StubEndpointStrategy implements EndpointStrategy {

    private final SoapServiceProperties soapServiceProperies;

    @Override
    public boolean supports(String appName) {
        // 모든 appName 지원 (cmpnCd로 판단)
        return true;
    }

    @Override
    public String determineEndpoint(SvcRequestInfoDTO svcRequestInfo, HttpHeaders headers) {
        String cmpnCd = headers.getFirst(HeaderConstants.CMPN_CD);

        // 성능테스트용 임시 로직: B로 시작하는 cmpnCd는 Stub 사용
        if (cmpnCd != null && cmpnCd.startsWith("B")) {
            return soapServiceProperies.getStubEndPoint();
        }

        return null; // 다른 전략으로 위임
    }

    @Override
    public int getPriority() {
        return 1; // B로 시작하면 무조건 Stub (성능테스트 우선)
    }
}