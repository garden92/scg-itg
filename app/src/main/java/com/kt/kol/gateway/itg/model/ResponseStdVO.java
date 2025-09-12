package com.kt.kol.gateway.itg.model;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "KOL ITG 공통 응답 모델")
public record ResponseStdVO(
		@Schema(title = "응답 유형", description = "I: 정상, E: 비즈니스에러, S: 시스템에러", example = "I") ResponseType responseType,
		@Schema(title = "응답 코드", description = "AnyLink, InfiniLink 에러코드") String responseCode,
		@Schema(title = "응답 제목") String responseTitle,
		@Schema(title = "응답 기본") String responseBasc,
		@Schema(title = "응답 상세") String responseDtal,
		@Schema(title = "응답 시스템") String responseSystem,

		@Schema(title = "응답 데이터") JsonNode data) {
	// 응답 유형을 enum으로 정의
	public enum ResponseType {
		I("정상"),
		E("비즈니스에러"),
		S("시스템에러");

		private final String description;

		ResponseType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static ResponseStdVO success(JsonNode data) {
		return new ResponseStdVO(
				ResponseType.I,
				"",
				"",
				"",
				"",
				"",
				data);
	}

	public static ResponseStdVO businessError(String responseCode, String responseTitle, String responseBasc,
			String responseDtal, String responseSystem) {
		return new ResponseStdVO(
				ResponseType.E,
				responseCode,
				responseTitle,
				responseBasc,
				responseDtal,
				responseSystem,
				null);
	}

	public static ResponseStdVO systemError(String responseCode, String responseTitle, String responseBasc,
			String responseDtal, String responseSyste) {
		return new ResponseStdVO(
				ResponseType.S,
				responseCode,
				responseTitle,
				responseBasc,
				responseDtal,
				responseSyste,
				null);
	}
}
