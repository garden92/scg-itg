package com.kt.kol.common.util;

import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.kt.kol.common.exception.BusinessExceptionWithRequestBody;
import com.kt.kol.common.model.soap.SoapEnvelope;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

public final class JaxbXmlSerializer {

    private static final ConcurrentMap<Class<?>, JAXBContext> CONTEXT_CACHE = new ConcurrentHashMap<>();

    private JaxbXmlSerializer() {
    }

    private static JAXBContext contextFor(Class<?> type) {
        try {
            return CONTEXT_CACHE.computeIfAbsent(type, k -> {
                try {
                    // 패키지 단위가 필요하면: JAXBContext.newInstance("com.kt.kol.common.model.soap");
                    return JAXBContext.newInstance(k);
                } catch (JAXBException e) {
                    throw new IllegalStateException("Create JAXBContext failed for " + k.getName(), e);
                }
            });
        } catch (RuntimeException re) {
            // computeIfAbsent 예외 래핑 방지
            Throwable cause = re.getCause() != null ? re.getCause() : re;
            if (cause instanceof IllegalStateException)
                throw (IllegalStateException) cause;
            throw re;
        }
    }

    /** SoapEnvelope → pretty XML (UTF-8, XML 선언 포함) */
    public static String toXMLString(SoapEnvelope soapEnvelope) {
        try {
            JAXBContext ctx = contextFor(SoapEnvelope.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // XML 선언 포함 (기본값). 선언 제거하려면: m.setProperty(Marshaller.JAXB_FRAGMENT,
            // Boolean.TRUE);

            StringWriter sw = new StringWriter();
            m.marshal(soapEnvelope, sw);
            return sw.toString();

        } catch (JAXBException e) {
            String detail = (e.getLinkedException() != null) ? e.getLinkedException().getMessage() : e.getMessage();
            String msg = (detail != null) ? ("XML 직렬화 오류: " + detail) : "XML 직렬화 중 오류 발생";
            throw new BusinessExceptionWithRequestBody(msg, e);
        }
    }

    /** 임의 객체 → XML(UTF-8, 헤더 제거: JAXB_FRAGMENT=true) */
    public static <T> String toXML(T object) {
        if (object == null)
            return "";
        try {
            JAXBContext ctx = contextFor(object.getClass());
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE); // XML 선언 제거

            StringWriter sw = new StringWriter();
            m.marshal(object, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new BusinessExceptionWithRequestBody(
                    "Failed to convert " + object.getClass().getSimpleName() + " to XML", e);
        }
    }
}