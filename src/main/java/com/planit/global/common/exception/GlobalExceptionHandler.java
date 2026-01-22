package com.planit.global.common.exception; //.global exception package

import com.planit.global.common.response.ErrorResponse; // 자체 ErrorResponse DTO
import jakarta.validation.ConstraintViolation; // constraint violation descriptor
import jakarta.validation.ConstraintViolationException; // validation exception type
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice // 전역 예외 처리 컨트롤러
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        // Spring MVC의 @Valid 검증 실패 시 처리
        return buildFromFieldErrors(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        // @Validated + Form binding 과정에서 발생한 BindException 처리
        return buildFromFieldErrors(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        // MethodValidation 등 constraint violation 처리
        List<ErrorResponse.FieldError> errors = ex.getConstraintViolations()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        String message = errors.isEmpty() ? "Validation failed" : errors.get(0).getMessage();
        return ResponseEntity.badRequest().body(ErrorResponse.of(message, errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        // ResponseStatusException으로 던져진 HTTP 상태를 그대로 사용
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return ResponseEntity.status(status).body(ErrorResponse.of(message));
    }

    private ResponseEntity<ErrorResponse> buildFromFieldErrors(List<FieldError> fieldErrors) {
        // FieldError를 ErrorResponse.FieldError로 변환
        List<ErrorResponse.FieldError> errors = fieldErrors.stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        String message = errors.isEmpty() ? "Validation failed" : errors.get(0).getMessage();
        return ResponseEntity.badRequest().body(ErrorResponse.of(message, errors));
    }

    private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        // ConstraintViolation의 프로퍼티 경로와 메시지를 추출
        String field = violation.getPropertyPath().toString();
        return new ErrorResponse.FieldError(field, violation.getMessage());
    }
}
