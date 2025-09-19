package com.kt.kol.common.model.soap;

import com.kt.kol.common.util.JaxbXmlSerializer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "bizHeader")
public class BizHeader {

    private String orderId;
    private String cbSvcName;
    private String cbFnName;

    public String toXML() {
        return JaxbXmlSerializer.toXML(this); // 유틸리티 메서드 호출
    }
}
