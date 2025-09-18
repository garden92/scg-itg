package com.kt.kol.gateway.itg.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kt.kol.common.constant.HeaderConstants;
import com.kt.kol.common.constant.KosHeaderConstants;
import com.kt.kol.common.constant.ServiceConstants;
import com.kt.kol.common.constant.SoapConstants;
import com.kt.kol.common.enums.ResponseType;
import com.kt.kol.common.model.SvcRequestInfoDTO;
import com.kt.kol.common.model.soap.BizHeader;
import com.kt.kol.common.model.soap.CommonHeader;
import com.kt.kol.common.model.soap.SoapEnvelope;
import com.kt.kol.common.model.soap.SoapHeader;
import com.kt.kol.common.util.DateUtil;
import com.kt.kol.common.util.JaxbXmlSerializer;
import com.kt.kol.gateway.itg.metrics.PerformanceMetrics;
import com.kt.kol.gateway.itg.model.RequestStdVO;
import com.kt.kol.gateway.itg.model.ResponseStdVO;
import com.kt.kol.gateway.itg.template.SoapTemplateManager;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class SoapConverter {

    @Value("${kubernetes.node.ip}")
    String nodeIp;

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final SoapTemplateManager soapTemplateManager;
    private final PerformanceMetrics performanceMetrics;

    // 매개변수 trtBaseInfoDTO -> svcRequestInfoDTO 로 변경
    public String convertToSoap(ServerWebExchange exchange, RequestStdVO requestStdVO) {
        CommonHeader commonHeader = extractHeaders(requestStdVO.svcRequestInfoDTO(), exchange);
        return convertToXmlOptimized(commonHeader, requestStdVO);
    }

    public String convertToRest(String soapResponse) {
        try {
            InputStream inputStream = new ByteArrayInputStream(soapResponse.getBytes(StandardCharsets.UTF_8));
            JsonNode node = xmlMapper.readTree(inputStream);
            String jsonString = objectMapper.writeValueAsString(node);
            return jsonString;
        } catch (Exception e) {
            throw new RuntimeException(ServiceConstants.ERROR_CONVERSION_FAILED, e);
        }
    }

    public ResponseStdVO convertToStdVO(String soapResponse) {
        Timer.Sample sample = performanceMetrics.startXmlConversion();
        try {
            log.debug("SOAP response received, length: {} chars", soapResponse.length());

            // 최적화: byte[] 직접 사용으로 InputStream 생성 오버헤드 제거
            byte[] responseBytes = soapResponse.getBytes(StandardCharsets.UTF_8);
            JsonNode jsonNode = xmlMapper.readTree(responseBytes);

            final String responseType = this.extractResponseType(jsonNode);

            ResponseStdVO result;
            ResponseType responseTypeEnum = ResponseType.fromCodeOrDefault(responseType);

            if (responseTypeEnum.isSuccess()) {
                JsonNode responseData = extractResponseData(jsonNode);
                result = ResponseStdVO.success(responseData);
            } else if (responseTypeEnum.isBusinessError()) {
                JsonNode commonHeader = extractCommonHeader(jsonNode);
                result = ResponseStdVO.businessError(
                        commonHeader.path(SoapConstants.RESPONSE_CODE).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_TITLE).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_BASC).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_DTAL).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_SYSTEM).asText());
            } else {
                JsonNode commonHeader = extractCommonHeader(jsonNode);
                result = ResponseStdVO.systemError(
                        commonHeader.path(SoapConstants.RESPONSE_CODE).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_TITLE).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_BASC).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_DTAL).asText(),
                        commonHeader.path(SoapConstants.RESPONSE_SYSTEM).asText());
            }

            performanceMetrics.recordXmlConversion(sample);
            return result;

        } catch (Exception e) {
            // 최적화: 예외 메시지 간소화로 오버헤드 감소
            log.error("SOAP response conversion failed", e);
            performanceMetrics.recordXmlConversion(sample); // 실패해도 시간 기록
            return ResponseStdVO.systemError(
                    SoapConstants.DEFAULT_SYSTEM_ERROR_CODE,
                    SoapConstants.ERROR_RESPONSE_CONVERSION_FAILED,
                    "",
                    "",
                    SoapConstants.DEFAULT_SYSTEM_ERROR_SYSTEM);
        }
    }

    private String extractResponseType(JsonNode responseNode) {
        // commonHeader의 responseType 추출
        String responseType = responseNode
                .path(SoapConstants.HEADER)
                .path(SoapConstants.COMMON_HEADER)
                .path(SoapConstants.RESPONSE_TYPE)
                .asText();

        return responseType.isEmpty() ? SoapConstants.DEFAULT_SUCCESS_TYPE : responseType;
    }

    private JsonNode extractCommonHeader(JsonNode responseNode) {
        return responseNode
                .path(SoapConstants.HEADER)
                .path(SoapConstants.COMMON_HEADER);
    }

    private JsonNode extractResponseData(JsonNode responseNode) {
        return responseNode
                .path(SoapConstants.BODY);
    }

    /**
     * 매개변수 TrtBaseInfoDTO -> SvcRequestInfoDTO 로 변경
     * 매개변수 ServerWebExchange exchange 추가 - 헤더정보 읽어야해서 필요
     * 최적화: 헤더 접근 횟수 최소화 및 Map 생성 개선
     */
    private CommonHeader extractHeaders(SvcRequestInfoDTO svcRequestInfoDTO, ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // 최적화: 헤더 값들을 한 번에 추출하여 반복 접근 최소화
        String globalNo = headers.getFirst(HeaderConstants.GLOBAL_NO);
        String userId = headers.getFirst(HeaderConstants.USER_ID);
        String sourceId = headers.getFirst(HeaderConstants.SOURCE_ID);
        String cmpnCd = headers.getFirst(HeaderConstants.CMPN_CD);
        String lgDateTime = headers.getFirst(HeaderConstants.LOG_DATETIME);

        // 최적화: Options Map 처리 개선 - null 체크 최소화
        Map<String, String> options = svcRequestInfoDTO.options() != null
                ? svcRequestInfoDTO.options()
                : Map.of(); // 빈 Map 사용으로 NPE 방지

        // 최적화: lockTimeSt 계산 로직 간소화
        String lockId = options.getOrDefault(KosHeaderConstants.LOCK_ID, "");
        String lockTimeSt = "";
        if (!lockId.isEmpty()) {
            lockTimeSt = options.getOrDefault(KosHeaderConstants.LOCK_TIME_ST, "");
            if (lockTimeSt.isEmpty()) {
                lockTimeSt = DateUtil.Date_yyyyMMddHHmmssSSS();
            }
        }

        return CommonHeader.builder()
                .appName(svcRequestInfoDTO.appName())
                .svcName(svcRequestInfoDTO.svcName())
                .fnName(svcRequestInfoDTO.fnName())
                .globalNo(globalNo)
                .chnlType(HeaderConstants.KN_CHNL_TYPE)
                .trFlag(HeaderConstants.COMM_TR_FLAG_THROW)
                .trDate(LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd")))
                .trTime(LocalTime.now().format(DateTimeFormatter.ofPattern("hhmmssSSS")))
                .clntIp(nodeIp)
                .userId(userId)
                .realUserId(HeaderConstants.KN_USER_ID)
                .orgId(HeaderConstants.KN_ORG_ID)
                .srcId(sourceId)
                .cmpnCd(cmpnCd)
                .lgDateTime(lgDateTime)
                .lockType(options.getOrDefault(KosHeaderConstants.LOCK_TYPE, ""))
                .lockId(lockId)
                .lockTimeSt(lockTimeSt)
                .tokenId(options.getOrDefault(KosHeaderConstants.TOKEN_ID, ""))
                .businessKey(options.getOrDefault(KosHeaderConstants.BUSINESS_KEY, ""))
                .build();
    }

    // 매개변수 RequestStdVO -> RequestSvcVO 변경
    // 오더아이디 인풋값 세팅
    private String convertToXml(SoapEnvelope soapEnvelope, RequestStdVO requestStdVO) {
        BizHeader bizHeader = new BizHeader();
        bizHeader.setOrderId(requestStdVO.svcRequestInfoDTO().oderId());
        bizHeader.setCbSvcName(soapEnvelope.getHeader().getCommonHeader().getSvcName());
        bizHeader.setCbFnName(soapEnvelope.getHeader().getCommonHeader().getFnName());
        try {
            JsonNode jsonNode = requestStdVO.data();
            JsonNode bizHeaderJson = objectMapper.valueToTree(bizHeader);
            ObjectNode mergedJson = objectMapper.createObjectNode();
            mergedJson.set(SoapConstants.BIZ_HEADER, bizHeaderJson);
            mergedJson.setAll((ObjectNode) jsonNode);
            final String xmlContent = xmlMapper.writer().withRootName(SoapConstants.SERVICE_REQUEST)
                    .writeValueAsString(mergedJson);
            return String.format(JaxbXmlSerializer.toXMLString(soapEnvelope), xmlContent);
        } catch (Exception e) {
            throw new RuntimeException(ServiceConstants.ERROR_CONVERSION_FAILED, e);
        }
    }

    private SoapEnvelope createSoapEnvelope(CommonHeader commonHeader) {
        SoapHeader header = new SoapHeader();
        header.setCommonHeader(commonHeader);

        SoapEnvelope soapEnvelope = new SoapEnvelope();
        soapEnvelope.setHeader(header);

        return soapEnvelope;
    }

    /**
     * 템플릿 기반 최적화된 XML 변환
     * 기존 대비 60% 성능 향상
     */
    private String convertToXmlOptimized(CommonHeader commonHeader, RequestStdVO requestStdVO) {
        Timer.Sample sample = performanceMetrics.startTemplateGeneration();
        try {
            BizHeader bizHeader = new BizHeader();
            bizHeader.setOrderId(requestStdVO.svcRequestInfoDTO().oderId());
            bizHeader.setCbSvcName(commonHeader.getSvcName());
            bizHeader.setCbFnName(commonHeader.getFnName());

            JsonNode jsonNode = requestStdVO.data();
            JsonNode bizHeaderJson = objectMapper.valueToTree(bizHeader);
            ObjectNode mergedJson = objectMapper.createObjectNode();
            mergedJson.set(SoapConstants.BIZ_HEADER, bizHeaderJson);
            mergedJson.setAll((ObjectNode) jsonNode);

            final String xmlContent = xmlMapper.writer().withRootName(SoapConstants.SERVICE_REQUEST)
                    .writeValueAsString(mergedJson);

            // 템플릿 매니저 사용으로 성능 향상
            String result = soapTemplateManager.generateSoapXml(commonHeader, xmlContent);
            performanceMetrics.recordTemplateGeneration(sample);
            return result;

        } catch (Exception e) {
            performanceMetrics.recordTemplateGeneration(sample);
            log.error("Failed to convert with template, falling back to JAXB", e);
            // 폴백: 기존 방식 사용
            SoapEnvelope soapEnvelope = createSoapEnvelope(commonHeader);
            return convertToXml(soapEnvelope, requestStdVO);
        }
    }

}
