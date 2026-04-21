package com.goggles.common.exception;

import com.goggles.common.response.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice("com.goggles")
public class GlobalExceptionAdvice {

	// 직접 정의한 CustomException 처리
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
		// MDC에서 traceId를 가져와 로그에 명시적으로 출력
		log.error("[TraceID: {}] Custom Exception: {}", MDC.get("traceId"), e.getMessage(), e);
		return ResponseEntity.status(e.getStatus())
				.body(ErrorResponse.of(e.getStatus(), e.getField(), e.getMessage()));
	}

	// Bean Validation (@Valid) 실패 시 처리
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException e) {
		log.error("[TraceID: {}] Validation Exception: {} errors found", MDC.get("traceId"),
				e.getBindingResult()
						.getErrorCount(), e);

		Map<String, String> errors = e.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(FieldError::getField,
						fieldError -> fieldError.getDefaultMessage() != null ?
								fieldError.getDefaultMessage() : "Invalid value",
						(existing, replacement) -> existing + ", " + replacement));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST, errors));
	}

	/**
	 * 제약 조건 위반 (@Validated) 처리 보강
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(
			ConstraintViolationException e) {
		log.error("[TraceID: {}] Constraint Violation: {}", MDC.get("traceId"), e.getMessage(), e);

		// PathVariable 등에서 발생하는 에러를 필드 단위로 상세화
		Map<String, String> errors = e.getConstraintViolations()
				.stream()
				.collect(Collectors.toMap(violation -> {
							String path = violation.getPropertyPath()
									.toString();
							return path.substring(path.lastIndexOf('.') + 1);
						}, ConstraintViolation::getMessage,
						(existing, replacement) -> existing + ", " + replacement));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST, errors));
	}

	// HTTP 요청 바디(Body)가 없거나 읽을 수 없는 경우
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
			HttpMessageNotReadableException e) {
		log.error("[TraceID: {}] Message Not Readable: {}", MDC.get("traceId"), e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "요청 본문(Body)이 누락되었거나 형식이 잘못되었습니다."));
	}

	// 잘못된 인자 전달 (IllegalArgumentException)
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
			IllegalArgumentException e) {
		log.error("[TraceID: {}] Illegal Argument: {}", MDC.get("traceId"), e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "잘못된 요청 값: " + e.getMessage()));
	}

	// 낙관적 락 충돌 (동시성 제어 실패)
	@ExceptionHandler({OptimisticLockException.class,
			ObjectOptimisticLockingFailureException.class})
	public ResponseEntity<ErrorResponse> handleOptimisticLockException(Exception e) {
		log.error("[TraceID: {}] Optimistic Lock Error: {}", MDC.get("traceId"), e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(HttpStatus.CONFLICT, "데이터 변경 중에 충돌이 발생했습니다. 다시 시도해주세요."));
	}

	// 그 외 정의되지 않은 모든 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAllException(Exception e) {
		// 시스템 예외는 추적을 위해 StackTrace 전체와 TraceID를 기록
		log.error("[TraceID: {}] Unhandled Exception occurred", MDC.get("traceId"), e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."));
	}
}