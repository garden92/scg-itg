package com.kt.kol.gateway.itg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.kt.kol.common.model.SvcRequestInfoDTO;

public record RequestStdVO(SvcRequestInfoDTO svcRequestInfoDTO,
        JsonNode data) {
}
