package com.kt.kol.common.code;

import lombok.Getter;

@Getter
public enum ErrorCode {
	INVALID_INPUT(400, "I");
	
	private final int status;
	private final String message;
	
	ErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

}
