package com.planit.global.common.exception;

public class UnauthorizedAccessException extends RuntimeException {

    private final ErrorCode errorCode;

    public UnauthorizedAccessException() {
        this(ErrorCode.USER_UNAUTHORIZED);
    }

    public UnauthorizedAccessException(String message) {
        this(ErrorCode.USER_UNAUTHORIZED, message);
    }

    public UnauthorizedAccessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public UnauthorizedAccessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
