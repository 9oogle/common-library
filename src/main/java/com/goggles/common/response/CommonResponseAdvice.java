package com.goggles.common.response;

import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.goggles")
public class CommonResponseAdvice implements ResponseBodyAdvice<Object>  {
	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		Class<?> type = returnType.getParameterType();

		// 이미 규격화된 응답이거나 특수 타입인 경우 제외
		return !(type.equals(ApiResponse.class) ||
				type.equals(ErrorResponse.class) ||
				type.equals(ResponseEntity.class) ||
				type.equals(String.class) ||
				type.equals(Void.class) ||
				type.isPrimitive()); // 기본 자료형 반환 시에도 주의 필요
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		// 컨트롤러가 반환한 데이터를 ApiResponse.success() 메서드로 감싸서 반환
		return new ApiResponse<>(
				true,
				"요청이 성공적으로 처리되었습니다.",
				body,
				MDC.get("traceId")
		);
	}
}
