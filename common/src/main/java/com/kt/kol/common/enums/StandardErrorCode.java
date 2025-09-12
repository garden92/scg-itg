package com.kt.kol.common.enums;

import lombok.Getter;

/**
 * Standard Error Code enumeration for consistent error code handling
 * Used in error responses and exception handling
 */
@Getter
public enum StandardErrorCode {
    /**
     * System Error Code
     */
    KOL_SYSTEM_ERROR("KOL_SYS_ERR", "System Error"),
    
    /**
     * Success Response Code
     */
    SUCCESS("SUCCESS", "Success"),
    
    /**
     * Business Logic Error Code
     */
    BUSINESS_ERROR("BUSINESS_ERROR", "Business Logic Error"),
    
    /**
     * Response Conversion Error Code
     */
    CONVERSION_ERROR("CONV_ERR", "Response conversion error");
    
    private final String code;
    private final String message;
    
    StandardErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * Get error code string for external systems
     * 
     * @return error code string
     */
    public String getCode() {
        return this.code;
    }
    
    /**
     * Get human-readable error message
     * 
     * @return error message
     */
    public String getMessage() {
        return this.message;
    }
}