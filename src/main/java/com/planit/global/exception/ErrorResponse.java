package com.planit.global.exception;

import java.util.Collections;
import java.util.List;

public class ErrorResponse {
    private final String message;
    private final List<FieldError> errors;

    public ErrorResponse(String message, List<FieldError> errors) {
        this.message = message;
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Collections.emptyList());
    }

    public static ErrorResponse of(String message, List<FieldError> errors) {
        return new ErrorResponse(message, errors);
    }

    public String getMessage() {
        return message;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
