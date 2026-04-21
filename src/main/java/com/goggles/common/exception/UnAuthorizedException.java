package com.goggles.common.exception;

import org.springframework.http.HttpStatus;

public class UnAuthorizedException extends CustomException {
	public UnAuthorizedException() {
		this("로그인이 필요한 서비스입니다.");
	}

	public UnAuthorizedException(String message) {
		super(message, HttpStatus.UNAUTHORIZED);
	}
}
