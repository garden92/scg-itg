package com.kt.kol.common.enums;

import com.kt.kol.common.constant.SoapConstants;

/**
 * SOAP 응답 타입 Enum
 * 타입 안전성과 검증 메소드를 제공
 */
public enum ResponseType {
    
    SUCCESS(SoapConstants.DEFAULT_SUCCESS_TYPE, "성공"),
    BUSINESS_ERROR(SoapConstants.BUSINESS_ERROR_TYPE, "업무 오류"), 
    SYSTEM_ERROR(SoapConstants.SYSTEM_ERROR_TYPE, "시스템 오류");
    
    private final String code;
    private final String description;
    
    ResponseType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 코드와 일치하는지 확인
     */
    public boolean matches(String responseCode) {
        return this.code.equals(responseCode);
    }
    
    /**
     * 성공 응답인지 확인
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 에러 응답인지 확인  
     */
    public boolean isError() {
        return this == BUSINESS_ERROR || this == SYSTEM_ERROR;
    }
    
    /**
     * 업무 에러인지 확인
     */
    public boolean isBusinessError() {
        return this == BUSINESS_ERROR;
    }
    
    /**
     * 시스템 에러인지 확인
     */
    public boolean isSystemError() {
        return this == SYSTEM_ERROR;
    }
    
    /**
     * 코드로부터 ResponseType 조회
     */
    public static ResponseType fromCode(String code) {
        for (ResponseType type : values()) {
            if (type.matches(code)) {
                return type;
            }
        }
        return SYSTEM_ERROR; // 기본값
    }
    
    /**
     * 코드가 빈 값이거나 null인 경우 기본값 반환
     */
    public static ResponseType fromCodeOrDefault(String code) {
        if (code == null || code.isEmpty()) {
            return SUCCESS; // 기본값은 성공
        }
        return fromCode(code);
    }
}