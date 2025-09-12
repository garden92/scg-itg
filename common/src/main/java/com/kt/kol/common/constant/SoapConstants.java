package com.kt.kol.common.constant;

/**
 * SOAP XML 관련 상수
 */
public final class SoapConstants {
    
    // SOAP XML Elements
    public static final String ENVELOPE = "Envelope";
    public static final String HEADER = "Header";
    public static final String BODY = "Body";
    public static final String COMMON_HEADER = "commonHeader";
    public static final String BIZ_HEADER = "bizHeader";
    public static final String SERVICE_REQUEST = "service_request";
    public static final String SERVICE_RESPONSE = "service_response";
    
    // Response Fields
    public static final String RESPONSE_TYPE = "responseType";
    public static final String RESPONSE_CODE = "responseCode";
    public static final String RESPONSE_TITLE = "responseTitle";
    public static final String RESPONSE_BASC = "responseBasc";
    public static final String RESPONSE_DTAL = "responseDtal";
    public static final String RESPONSE_SYSTEM = "responseSystem";
    
    // SOAP Namespaces
    public static final String SOAP_ENV_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_ENV_PREFIX = "soap-env";
    
    // Default Values
    public static final String DEFAULT_RESPONSE_TYPE = "I";
    public static final String DEFAULT_SUCCESS_TYPE = "I";
    public static final String BUSINESS_ERROR_TYPE = "E";
    public static final String SYSTEM_ERROR_TYPE = "S";
    
    // Error Codes
    public static final String DEFAULT_SYSTEM_ERROR_CODE = "KOL_SYS_ERR";
    public static final String DEFAULT_SYSTEM_ERROR_TITLE = "System Error";
    public static final String DEFAULT_SYSTEM_ERROR_SYSTEM = "KOL";
    public static final String ERROR_RESPONSE_CONVERSION_FAILED = "Response conversion error";
    
    private SoapConstants() {
        // Utility class - prevent instantiation
    }
}