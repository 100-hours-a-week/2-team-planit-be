package com.planit.global.common.exception;

import com.planit.domain.user.exception.DuplicateLoginIdException;
import com.planit.domain.user.exception.DuplicateNicknameException;
import com.planit.domain.keywordalert.exception.DuplicateKeywordException;
import com.planit.global.common.response.ErrorResponse;
import com.planit.global.common.exception.UnauthorizedAccessException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException ex) {
        logger.warn("DuplicateNickname", ex);
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateLoginId(DuplicateLoginIdException ex) {
        logger.warn("DuplicateLoginId", ex);
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler(DuplicateKeywordException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKeyword(DuplicateKeywordException ex) {
        logger.warn("DuplicateKeyword", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        logger.warn("BusinessException", ex);
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAccessException ex) {
        logger.warn("UnauthorizedAccess", ex);
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("MethodArgumentNotValidException", ex);
        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(ErrorCode.INVALID_INPUT, errors));
    }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInputException(Exception ex) {
        logger.warn("InvalidInputException", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        logger.warn("UnsupportedMediaType", ex);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.from(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("IllegalArgument", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        logger.warn("ResponseStatusException", ex);
        String message = ex.getReason() != null ? ex.getReason() : ErrorCode.INVALID_INPUT.getMessage();
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("message", message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        logger.error("🔥 JWT KEY ERROR: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(ErrorCode.COMMON_999));
    }

    @ExceptionHandler({NullPointerException.class, SQLException.class})
    public ResponseEntity<ErrorResponse> handleCriticalException(RuntimeException ex) {
        logger.error("CriticalException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(ErrorCode.COMMON_999));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        logger.error("UnexpectedException", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(ErrorCode.COMMON_999));
    }
}
