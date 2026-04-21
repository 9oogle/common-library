package com.goggles.common.response;

import org.slf4j.MDC;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String traceId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data, MDC.get("traceId"));
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, MDC.get("traceId"));
    }
}