package com.kt.kol.common.feign;

import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

import feign.Client;
import feign.Logger;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jaxb.JAXBContextFactory;
import feign.jaxb.JAXBDecoder;
import feign.jaxb.JAXBEncoder;
import feign.okhttp.OkHttpClient;

@Configuration
public class SoapClientConfig {

	private final JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder().withMarshallerJAXBEncoding("UTF-8")
			.build();

	@Bean
	FeignFormatterRegistrar localDateFeignFormatterRegister() {
		return registry -> {
			DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
			registrar.setUseIsoFormat(true);
			registrar.registerFormatters(registry);
		};
	}
	
	@Bean
	Retryer feignRetryer() {
		return new Retryer.Default(1000, 2000, 3);
	}

	@Bean
	Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}

	@Bean
	Client feignClient() {
		return new OkHttpClient();
	}

	//	@Bean
	// TODO: 확인해서 사용 할 수 있게 할 것
	Encoder feignEncoder() {
		return new JAXBEncoder(jaxbFactory);
	}

	//	@Bean
	// TODO: 확인해서 사용 할 수 있게 할 것
	Decoder feignJaxbDecoder() {
		return new JAXBDecoder(jaxbFactory);
	}

}
