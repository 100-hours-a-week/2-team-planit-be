package com.planit.global.common.exception;

import com.planit.domain.user.exception.DuplicateLoginIdException;
import com.planit.domain.user.exception.DuplicateNicknameException;
import com.planit.global.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.sql.SQLException;
import org.springframework.web.bind.annotation.ControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException ex) {
        logger.warn("DuplicateNickname", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateLoginId(DuplicateLoginIdException ex) {
        logger.warn("DuplicateLoginId", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        logger.warn("BusinessException", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.from(ex.getErrorCode()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
        logger.warn("ValidationException", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(ErrorCode.COMMON_001));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        logger.warn("UnsupportedMediaType", ex);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.from(ErrorCode.COMMON_001));
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
