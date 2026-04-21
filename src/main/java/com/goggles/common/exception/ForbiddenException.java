package com.goggles.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends CustomException {
    public ForbiddenException() {
        this("해당 작업에 대한 접근 권한이 없습니다.");
    }
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
