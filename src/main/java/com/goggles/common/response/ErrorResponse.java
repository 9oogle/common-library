package com.goggles.common.response;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;

/**
 * 에러 상세 정보 DTO.
 */

public record ErrorResponse(int status, String error, Object message, String field, String traceId,
							LocalDateTime timestamp) {

	public static ErrorResponse of(HttpStatusCode status, Object message) {
		return new ErrorResponse(status.value(), HttpStatus.valueOf(status.value())
				.name(), message, null, MDC.get("traceId"), LocalDateTime.now());
	}

	public static ErrorResponse of(HttpStatusCode status, String field, Object message) {
		return new ErrorResponse(status.value(), HttpStatus.valueOf(status.value())
				.name(), message, field, MDC.get("traceId"), LocalDateTime.now());
	}
}