package com.planit.domain.place.exception;

import com.planit.global.common.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PlaceSearchExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PlaceSearchExceptionHandler.class);

    @ExceptionHandler(PlaceSearchException.class)
    public ResponseEntity<ErrorResponse> handlePlaceSearchException(PlaceSearchException ex) {
        logger.warn("PlaceSearchException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponse.from(ex.getErrorCode()));
    }
}
