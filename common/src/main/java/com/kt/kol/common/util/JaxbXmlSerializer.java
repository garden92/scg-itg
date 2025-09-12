package com.kt.kol.common.util;

import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.kt.kol.common.exception.BusinessExceptionWithReqeustBody;
import com.kt.kol.common.model.soap.SoapEnvelope;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JaxbXmlSerializer {
    private static final ConcurrentMap<String, JAXBContext> contextCache = new ConcurrentHashMap<>();

    public static <T> String toXMLString(SoapEnvelope soapEnvelope) {
        try {
            String cacheKey = SoapEnvelope.class.getName();
            JAXBContext context = contextCache.computeIfAbsent(cacheKey, k -> {
                try {
                    return JAXBContext.newInstance(SoapEnvelope.class);
                } catch (JAXBException e) {
                    log.error(e.toString());
                    throw new RuntimeException(
                            "Failed to convert toXMLString to XML", e);
                }
            });

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(soapEnvelope, stringWriter);
            return stringWriter.toString();

        } catch (JAXBException e) {
            String errorMessage;
            if (e.getLinkedException() != null) {
                errorMessage = String.format("XML 직렬화 중 세부 오류: %s",
                        e.getLinkedException().getMessage());
            } else {
                errorMessage = "XML 직렬화 중 오류 발생";
            }
            log.error(errorMessage, e);
            throw new BusinessExceptionWithReqeustBody(errorMessage, e);
        }
    }

    public static <T> String toXML(T object) {
        try {
            JAXBContext context = JAXBContext.newInstance(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            // XML 헤더 제거 설정
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(object, stringWriter);
            return stringWriter.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(
                    "Failed to convert " + object.getClass().getSimpleName() + " to XML", e);
        }
    }
}