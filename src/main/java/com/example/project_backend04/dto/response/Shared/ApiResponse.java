package com.example.project_backend04.dto.response.Shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private int status;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data, 200);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return new ApiResponse<>(false, message, null, status);
    }

    /** Dùng khi ApiResponse<Boolean> */
    public boolean isTrue() {
        return Boolean.TRUE.equals(this.data);
    }

    public boolean isFalse() {
        return !Boolean.TRUE.equals(this.data);
    }

    public T getOrDefault(T defaultValue) {
        return data != null ? data : defaultValue;
    }
}
