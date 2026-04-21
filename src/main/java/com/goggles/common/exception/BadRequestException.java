package com.goggles.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadRequestException extends CustomException {

	public BadRequestException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}

	public BadRequestException(String field, String message) {
		super(field, message, HttpStatus.BAD_REQUEST);

	}
}