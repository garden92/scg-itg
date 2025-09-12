package com.kt.kol.common.constant;

/**
 * Gateway 라우트 관련 상수
 */
public final class RouteConstants {
    
    // Route IDs
    public static final String REST_SOAP_PO_ROUTE = "rest-soap-po-route";
    public static final String REST_SOAP_ESB_ROUTE = "rest-soap-esb-route";
    
    // Route Paths
    public static final String SOAP_DYNAMIC_GATEWAY_PATH = "/SoapDynamicGateway";
    public static final String SOAP_GATEWAY_PATH = "/SoapGateway";
    
    // Special URIs
    public static final String NO_OP_URI = "no://op";
    
    // Fallback Paths
    public static final String CIRCUIT_BREAKER_FALLBACK_PATH = "/circuitbreakerfallback";
    
    private RouteConstants() {
        // Utility class - prevent instantiation
    }
}