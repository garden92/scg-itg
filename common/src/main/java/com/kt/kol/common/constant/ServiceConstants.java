package com.kt.kol.common.constant;

/**
 * 서비스 관련 상수
 */
public final class ServiceConstants {
    
    // Service Types
    public static final String SERVICE_TYPE_ORD = "ORD";
    public static final String SERVICE_TYPE_CRM = "CRM";
    
    // Endpoint Types  
    public static final String ENDPOINT_TYPE_PO = "PO";
    public static final String ENDPOINT_TYPE_ESB = "ESB";
    public static final String ENDPOINT_TYPE_STUB = "STUB";
    
    // Error Messages
    public static final String ERROR_INVALID_REQUEST = "Invalid request format";
    public static final String ERROR_CONVERSION_FAILED = "Failed to convert SOAP to REST";
    public static final String ERROR_SERVICE_UNAVAILABLE = "Service temporarily unavailable";
    public static final String ERROR_TIMEOUT_EXCEEDED = "Request timeout exceeded";
    public static final String ERROR_RESPONSE_CONVERSION_FAILED = "Response conversion error";
    
    // Processing Messages
    public static final String MSG_REQUEST_RECEIVED = "Request received for processing";
    public static final String MSG_PROCESSING_COMPLETE = "Request processing completed";
    public static final String MSG_FALLBACK_TRIGGERED = "Circuit breaker fallback triggered";
    
    // Validation Messages
    public static final String VALIDATION_MISSING_CONTENT_TYPE = "Missing Content-Type header";
    public static final String VALIDATION_INVALID_CONTENT_TYPE = "Content-Type must be application/json";
    public static final String VALIDATION_EMPTY_BODY = "Request body cannot be empty";
    public static final String VALIDATION_INVALID_JSON = "Invalid JSON format in request body";
    
    private ServiceConstants() {
        // Utility class - prevent instantiation
    }
}