package com.goggles.config.common;

import com.goggles.common.exception.GlobalExceptionAdvice;
import com.goggles.common.response.CommonResponseAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebResponseConfig {

	@Bean
	public CommonResponseAdvice commonResponseAdvice() {
		return new CommonResponseAdvice();
	}

	@Bean
	public GlobalExceptionAdvice globalExceptionAdvice() {
		return new GlobalExceptionAdvice();
	}
}
