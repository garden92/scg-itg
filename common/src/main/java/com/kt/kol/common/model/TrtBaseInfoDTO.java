package com.kt.kol.common.model;

/**
 * @deprecated TrtBaseInfoDTO -> Header정보로 변경
 */
@Deprecated
public record TrtBaseInfoDTO(
        String appName, // application name
        String svcName, // service name
        String fnName, // function name
        String globalNo, // global number
        String chnlType, // 채널타입 (KOS 연동 채널 타입)
        String trFlag, // 처리Flag
        String trDate, // 처리일자
        String trTime, // 처리시간
        String clntIp, // client IP
        String userId, // 사용자아이디
        String orgId, // 조직아이디
        String srcId, // 화면아이디
        String lgDateTime, // 로지컬데이트타임
        String cmpnCd // 회사코드
) {
}
