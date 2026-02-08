package com.planit.domain.place.exception;

import com.planit.global.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class PlaceSearchException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public PlaceSearchException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
