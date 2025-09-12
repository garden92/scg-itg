package com.kt.kol.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	
	/**
	 * <pre>
	 * yyyyMMdd return
	 * </pre> 
	 * @return yyyyMMdd
	 */
	public static String Date_yyyyMMdd() {
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	
	    Date date = new Date();
	    String dateString = formatter.format(date);
	    return dateString;
	}
	
	/**
	 * <pre>
	 * HHmmssSSS return
	 * </pre> 
	 * @return HHmmssSSS
	 */
	public static String Date_HHmmssSSS() {
	    SimpleDateFormat formatter = new SimpleDateFormat("HHmmssSSS");
	
	    Date date = new Date();
	    String dateString = formatter.format(date);
	    return dateString;
	}
	
	/**
	 * <pre>
	 * yyyyMMddHHmmss return
	 * </pre>  
	 * @return yyyyMMddHHmmss
	 */
	public static String Date_yyyyMMddHHmmss() {
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	
	    Date date = new Date();
	    String dateString = formatter.format(date);
	    return dateString;
	}

	/**
	 * <pre>
	 * yyyyMMddHHmmssSSS return
	 * </pre>  
	 * @return yyyyMMddHHmmssSSS
	 */
	public static String Date_yyyyMMddHHmmssSSS() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Date date = new Date();
		String dateString = formatter.format(date);
		return dateString;
	}
}
