package com.kt.kol.gateway.itg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.kt.kol.common.model.SvcRequestInfoDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "KOL ITG 공통 요청 모델")
public record RequestStdVO (@Schema(title = "서비스 요청 정보") SvcRequestInfoDTO svcRequestInfoDTO,
    @Schema(title = "API 요청 데이터") JsonNode data) {
}
