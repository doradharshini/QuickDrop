package com.quickdrop.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

	record ApiError(int status, String error, String message) {
	}

	@ExceptionHandler(OrderNotFoundException.class)
	ResponseEntity<ApiError> notFound(OrderNotFoundException e) {
		return build(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(IllegalOrderTransitionException.class)
	ResponseEntity<ApiError> conflict(IllegalOrderTransitionException e) {
		return build(HttpStatus.CONFLICT, e.getMessage());
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new ApiError(status.value(), status.getReasonPhrase(), message));
	}
}
