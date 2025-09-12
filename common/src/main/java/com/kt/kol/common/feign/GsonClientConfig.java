package com.kt.kol.common.feign;

import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

import feign.Client;
import feign.Logger;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;

public class GsonClientConfig {
	
	FeignFormatterRegistrar localDateFeignFormatterRegister() {
		return registry -> {
			DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
			registrar.setUseIsoFormat(true);
			registrar.registerFormatters(registry);
		};
	}
	
	Retryer feignRetryer() {
		return new Retryer.Default(1000, 2000, 3);
	}
	
	Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}
	
	Client feignClient() {
		return new OkHttpClient();
	}
	
	Encoder feignEncoder() {
		return new GsonEncoder();
	}
	
	Decoder feignDecoder() {
		return new ResponseEntityDecoder(new GsonDecoder());
	}

}
