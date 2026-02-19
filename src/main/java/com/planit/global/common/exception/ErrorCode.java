package com.planit.global.common.exception;

public enum ErrorCode {
    COMMON_001("COMMON_001", "잘못된 요청입니다"),
    COMMON_999("COMMON_999", "서버 오류가 발생했습니다"),
    USER_DUPLICATE_NICKNAME("USER_DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    USER_DUPLICATE_LOGIN_ID("USER_DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 아이디입니다."),
    USER_001("USER_001", "존재하지 않는 사용자입니다"),
    USER_UNAUTHORIZED("USER_UNAUTHORIZED", "인증이 필요한 요청입니다"),
    TRIP_001("TRIP_001", "여행을 찾을 수 없습니다"),
    TRIP_002("TRIP_002", "이미 생성된 여행이 있습니다"),
    TRIP_003("TRIP_003", "일정을 찾을 수 없습니다"),
    TRIP_004("TRIP_004", "장소 일정을 찾을 수 없습니다"),
    TRIP_005("TRIP_005", "일정 생성 허용 시간이 아닙니다"),
    TRIP_006("TRIP_006", "여행 소유자가 아닙니다"),
    TRIP_007("TRIP_007", "하루에 한 번만 일정 생성이 가능합니다"),
    PLACE_001("PLACE_001", "지원하지 않는 destinationCode 입니다"),
    PLACE_002("PLACE_002", "검색어가 올바르지 않습니다"),
    PLACE_003("PLACE_003", "장소 검색 호출에 실패했습니다"),
    PLACE_004("PLACE_004", "장소 검색 요청이 시간 초과되었습니다"),
    TRIP_NOT_FOUND("TRIP_NOT_FOUND", "여행 정보를 찾을 수 없습니다"),
    FORBIDDEN_TRIP_ACCESS("FORBIDDEN_TRIP_ACCESS", "다른 사용자의 여행입니다"),
    TRIP_ALREADY_SHARED("TRIP_ALREADY_SHARED", "이미 공유된 여행입니다");

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
