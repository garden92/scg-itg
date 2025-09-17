package com.kt.kol.gateway.itg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.kol.gateway.itg.model.ResponseStdVO;
import com.kt.kol.gateway.itg.util.SoapConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 응답 작성 서비스
 * JSON 및 SOAP XML 응답 모두 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseWriterService {
    
    private final ObjectMapper objectMapper;
    private final SoapConverter soapConverter;
    
    /**
     * 응답 작성 - 요청 Content-Type에 따라 JSON 또는 XML 응답
     */
    public Mono<Void> writeResponse(ServerWebExchange exchange, ResponseStdVO response) {
        try {
            // 요청의 Content-Type 확인
            MediaType requestContentType = exchange.getRequest().getHeaders().getContentType();
            
            if (requestContentType != null && requestContentType.includes(MediaType.TEXT_XML)) {
                // SOAP XML 요청에 대해서는 SOAP XML 응답
                return writeSoapXmlResponse(exchange, response);
            } else {
                // JSON 요청에 대해서는 JSON 응답 (기본값)
                return writeJsonResponse(exchange, response);
            }
            
        } catch (Exception e) {
            log.error("Failed to write response", e);
            return Mono.error(new RuntimeException("Failed to write response", e));
        }
    }
    
    /**
     * JSON 응답 작성
     */
    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, ResponseStdVO response) {
        try {
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] responseBytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
            
            log.debug("Writing JSON response");
            return exchange.getResponse().writeWith(Mono.just(buffer));
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JSON response", e);
            return Mono.error(new RuntimeException("Failed to serialize JSON response", e));
        }
    }
    
    /**
     * SOAP XML 응답 작성
     */
    private Mono<Void> writeSoapXmlResponse(ServerWebExchange exchange, ResponseStdVO response) {
        try {
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_XML);
            
            // ResponseStdVO를 SOAP XML로 변환
            String soapXmlResponse = convertResponseToSoapXml(response);
            byte[] responseBytes = soapXmlResponse.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
            
            log.debug("Writing SOAP XML response");
            return exchange.getResponse().writeWith(Mono.just(buffer));
            
        } catch (Exception e) {
            log.error("Failed to create SOAP XML response", e);
            return Mono.error(new RuntimeException("Failed to create SOAP XML response", e));
        }
    }
    
    /**
     * ResponseStdVO를 SOAP XML 형식으로 변환
     */
    private String convertResponseToSoapXml(ResponseStdVO response) {
        try {
            // ResponseType enum을 String으로 변환
            String responseType = response.responseType().name(); // I, E, S
            String responseCode = response.responseCode() != null ? response.responseCode() : "";
            String responseTitle = response.responseTitle() != null ? response.responseTitle() : "";
            String responseBasc = response.responseBasc() != null ? response.responseBasc() : "";
            String responseDtal = response.responseDtal() != null ? response.responseDtal() : "";
            String responseSystem = response.responseSystem() != null ? response.responseSystem() : "";
            
            // SOAP Envelope 구조 생성
            StringBuilder soapResponse = new StringBuilder();
            soapResponse.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            soapResponse.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n");
            soapResponse.append("    <soapenv:Header>\n");
            soapResponse.append("        <commonHeader>\n");
            soapResponse.append("            <responseType>").append(responseType).append("</responseType>\n");
            soapResponse.append("            <responseCode>").append(responseCode).append("</responseCode>\n");
            soapResponse.append("            <responseTitle>").append(responseTitle).append("</responseTitle>\n");
            soapResponse.append("            <responseBasc>").append(responseBasc).append("</responseBasc>\n");
            soapResponse.append("            <responseDtal>").append(responseDtal).append("</responseDtal>\n");
            soapResponse.append("            <responseSystem>").append(responseSystem).append("</responseSystem>\n");
            soapResponse.append("        </commonHeader>\n");
            soapResponse.append("    </soapenv:Header>\n");
            soapResponse.append("    <soapenv:Body>\n");
            
            // Response 데이터 추가
            if (response.data() != null) {
                // JsonNode를 XML 형태로 간단하게 변환
                String jsonString = objectMapper.writeValueAsString(response.data());
                // JSON을 기본 XML 형태로 변환 (간단한 구현)
                String xmlContent = convertJsonToXml(jsonString);
                soapResponse.append("        ").append(xmlContent).append("\n");
            }
            
            soapResponse.append("    </soapenv:Body>\n");
            soapResponse.append("</soapenv:Envelope>");
            
            return soapResponse.toString();
            
        } catch (Exception e) {
            log.error("Failed to convert response to SOAP XML", e);
            // 에러 발생 시 기본 SOAP 에러 응답
            return createDefaultSoapErrorResponse(e.getMessage());
        }
    }
    
    /**
     * JSON을 간단한 XML로 변환 (기본적인 구현)
     */
    private String convertJsonToXml(String jsonString) {
        try {
            // 간단한 변환: 중괄호와 따옴표 제거, 콜론을 XML 태그로 변환
            // 실제 프로덕션에서는 더 정교한 JSON-to-XML 변환 필요
            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                String content = jsonString.substring(1, jsonString.length() - 1);
                // 매우 기본적인 변환 - 실제로는 Jackson XML mapper 사용 권장
                return "<responseData>" + content.replaceAll("\"", "") + "</responseData>";
            }
            return "<responseData>" + jsonString + "</responseData>";
        } catch (Exception e) {
            log.warn("Failed to convert JSON to XML, using fallback", e);
            return "<responseData>Data conversion failed</responseData>";
        }
    }
    
    /**
     * 기본 SOAP 에러 응답 생성
     */
    private String createDefaultSoapErrorResponse(String errorMessage) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Header>
                    <commonHeader>
                        <responseType>E</responseType>
                        <responseCode>SYS-E001</responseCode>
                        <responseTitle>시스템 오류</responseTitle>
                        <responseBasc>응답 생성 중 오류가 발생했습니다.</responseBasc>
                        <responseDtal>%s</responseDtal>
                        <responseSystem>GATEWAY</responseSystem>
                    </commonHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <error>
                        <message>%s</message>
                    </error>
                </soapenv:Body>
            </soapenv:Envelope>
            """, errorMessage, errorMessage);
    }
}