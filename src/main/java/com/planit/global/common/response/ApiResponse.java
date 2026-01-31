package com.planit.global.common.response;

public class ApiResponse<T> {

    private final String message;
    private final T data;

    private ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", data);
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
