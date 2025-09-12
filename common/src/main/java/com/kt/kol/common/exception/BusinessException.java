package com.kt.kol.common.exception;

public class BusinessException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2291025218301636662L;
	/** 에러코드 */
	private String errorCode;

	/** redirect 할 URL */
	private String redirectUrl;

	private String[] errorArgsMsg;

//	private EsbService esbService;

	public BusinessException() {

	}
	
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

	public BusinessException(String errorCode) {
		this.errorCode = errorCode;
	}

	public BusinessException(String errorCode, String redirectUrl) {
		this.errorCode = errorCode;
		this.redirectUrl = redirectUrl;
	}

	public BusinessException(String errorCode, String[] errorArgsMsg) {
		this.errorCode = errorCode;
		this.errorArgsMsg = errorArgsMsg;
	}

	public BusinessException(String errorCode, String[] errorArgsMsg, String redirectUrl) {
		this.errorCode = errorCode;
		this.errorArgsMsg = errorArgsMsg;
		this.redirectUrl = redirectUrl;
	}

//	public BusinessException(EsbService esbService, String errorCode) {
//		this.esbService = esbService;
//		this.errorCode = errorCode;
//	}
//
//	public BusinessException(EsbService esbService, String errorCode, String redirectUrl) {
//		this.esbService = esbService;
//		this.errorCode = errorCode;
//		this.redirectUrl = redirectUrl;
//	}
//
//	public BusinessException(EsbService esbService, String errorCode, String[] errorArgsMsg) {
//		this.esbService = esbService;
//		this.errorCode = errorCode;
//		this.errorArgsMsg = errorArgsMsg;
//	}
//
//	public BusinessException(EsbService esbService, String errorCode, String[] errorArgsMsg, String redirectUrl) {
//		this.esbService = esbService;
//		this.errorCode = errorCode;
//		this.errorArgsMsg = errorArgsMsg;
//		this.redirectUrl = redirectUrl;
//	}

	public String getErrorCode() {
		return this.errorCode;
	}

	public String getRedirectUrl() {
		return this.redirectUrl;
	}

	public String[] getErrorArgsMsg() {
		String[] errorMsgArray = null;
		if (this.errorArgsMsg != null) {
			errorMsgArray = new String[this.errorArgsMsg.length];
			System.arraycopy(this.errorArgsMsg, 0, errorMsgArray, 0, this.errorArgsMsg.length);
		}
		return errorMsgArray;
	}

	public void setErrorArgsMsg(String[] errorArgsMsg) {
		this.errorArgsMsg = java.util.Arrays.copyOf(errorArgsMsg, errorArgsMsg.length);
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

//	public EsbService getEsbService() {
//		return esbService;
//	}
//
//	public void setEsbService(EsbService esbService) {
//		this.esbService = esbService;
//	}

}
