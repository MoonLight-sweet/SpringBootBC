package com.codeclinic.common;

public record ApiResult<T>(int code, String message, T data) {
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static ApiResult<Void> successMessage(String message) {
        return new ApiResult<>(200, message, null);
    }

    public static ApiResult<Void> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
