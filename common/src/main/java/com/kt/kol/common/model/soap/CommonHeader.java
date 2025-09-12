package com.kt.kol.common.model.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
public class CommonHeader {
    private String appName;
    private String svcName;
    private String fnName;
    private String chnlType;
    private String orgId;
    private String userId;
    private String realUserId;
    private String clntIp;
    private String globalNo;
    private String filler;
    private String lgDateTime;
    private String srcId;
    private String trDate;
    private String trFlag;
    private String trTime;
    private String cmpnCd;
    private String lockType;
    private String lockId;
    private String lockTimeSt;
    private String tokenId;
    private String businessKey;
}
