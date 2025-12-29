package com.promlog.promlog.global.response;

public record ApiResponse<T>(boolean success, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
