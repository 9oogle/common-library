package com.goggles.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

	private final HttpStatus status;
	private final String field;

	public CustomException(String message, HttpStatus status) {
		super(message);
		this.status = (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR;
		this.field = null;
	}

	public CustomException(String field, String message, HttpStatus status) {
		super(message);
		this.status = (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR;
		this.field = field;
	}
}