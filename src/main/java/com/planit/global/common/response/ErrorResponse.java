package com.planit.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.planit.global.common.exception.ErrorCode;
import java.util.List;

public class ErrorResponse {

    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldError> errors;
    private final ErrorDetail error;

    private ErrorResponse(String code, String message, List<FieldError> errors, ErrorDetail error) {
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.error = error;
    }

    public static ErrorResponse from(ErrorCode errorCode) {
        return from(errorCode, List.of());
    }

    public static ErrorResponse from(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                errors,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage())
        );
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public ErrorDetail getError() {
        return error;
    }

    public static class FieldError {
        private final String field;
        private final String reason;

        public FieldError(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public String getField() {
            return field;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class ErrorDetail {
        private final String code;
        private final String message;

        public ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
