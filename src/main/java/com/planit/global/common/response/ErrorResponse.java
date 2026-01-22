package com.planit.global.common.response; // 전역 API 에러 응답 DTO 패키지

import java.util.Collections; // 빈 리스트 반환 유틸
import java.util.List; // 오류 필드 리스트 타입

public class ErrorResponse {
    private final String message; // 전반적인 에러 메시지
    private final List<FieldError> errors; // 세부 field-level 오류 목록

    public ErrorResponse(String message, List<FieldError> errors) {
        this.message = message;
        this.errors = errors == null ? Collections.emptyList() : errors; // null-safe 처리
    }

    public static ErrorResponse of(String message) {
        // errors 없이 메시지만 반환
        return new ErrorResponse(message, Collections.emptyList());
    }

    public static ErrorResponse of(String message, List<FieldError> errors) {
        // 전체 메시지 + 필드 오류 응답 생성
        return new ErrorResponse(message, errors);
    }

    public String getMessage() {
        return message; // 에러 메시지 조회
    }

    public List<FieldError> getErrors() {
        return errors; // 필드 오류 목록 반환
    }

    public static class FieldError {
        private final String field; // 문제가 된 필드명
        private final String message; // 해당 필드 메시지

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field; // 필드명 노출
        }

        public String getMessage() {
            return message; // 필드 오류 메시지 제공
        }
    }
}
