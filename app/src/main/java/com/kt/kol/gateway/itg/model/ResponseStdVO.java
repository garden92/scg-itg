package com.kt.kol.gateway.itg.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ResponseStdVO(
		ResponseType responseType,
		String responseCode,
		String responseTitle,
		String responseBasc,
		String responseDtal,
		String responseSystem,

		JsonNode data) {
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
