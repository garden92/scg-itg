package com.kt.kol.common.constant;

/**
 * HTTP 미디어 타입 및 헤더 관련 상수
 */
public final class MediaTypes {
    
    // Content Types
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final String TEXT_XML = "text/xml";
    public static final String TEXT_XML_UTF8 = "text/xml;charset=UTF-8";
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_SOAP_XML = "application/soap+xml";
    
    // Character Encodings
    public static final String CHARSET_UTF8 = "UTF-8";
    
    // HTTP Headers
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    
    // SOAP Action
    public static final String SOAP_ACTION_HEADER = "SOAPAction";
    
    private MediaTypes() {
        // Utility class - prevent instantiation
    }
}