package com.planit.global.common.response;

import com.planit.global.common.exception.ErrorCode;

public class ErrorResponse {

    private final String message;
    private final ErrorDetail error;

    private ErrorResponse(String message, ErrorDetail error) {
        this.message = message;
        this.error = error;
    }

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getCode(),
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage())
        );
    }

    public String getMessage() {
        return message;
    }

    public ErrorDetail getError() {
        return error;
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