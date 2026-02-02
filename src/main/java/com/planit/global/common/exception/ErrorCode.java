package com.planit.global.common.exception;

public enum ErrorCode {
    COMMON_001("COMMON_001", "잘못된 요청입니다"),
    COMMON_999("COMMON_999", "서버 오류가 발생했습니다"),
    USER_001("USER_001", "존재하지 않는 사용자입니다"),
    USER_DUPLICATE_NICKNAME("USER_DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    USER_DUPLICATE_LOGIN_ID("USER_DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 아이디입니다."),
    TRIP_001("TRIP_001", "여행을 찾을 수 없습니다");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
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
