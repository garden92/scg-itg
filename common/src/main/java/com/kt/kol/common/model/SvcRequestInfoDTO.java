package com.kt.kol.common.model;

import java.util.Map;
import java.util.HashMap;

public record SvcRequestInfoDTO(String appName, // application name
String svcName, // service name
String fnName, // function name
String oderId, // oderId
Map<String, String> options // lock관련, tokenId 등 commheader 설정값
) {
    //options 처리 이전에 구현한 소스 영향도 없기 위해 생성자 추가
    public SvcRequestInfoDTO(String appName, String svcName, String fnName, String oderId) {
        this(appName, svcName, fnName, oderId, new HashMap<>());
    }
}
