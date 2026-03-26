package com.example.project_backend04.dto.response.Shared;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private int status;
    private String errorCode;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, T data, int status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data, 200);
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, 200);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, int status, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .status(status)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    @JsonIgnore
    public boolean isTrue() {
        return Boolean.TRUE.equals(this.data);
    }

    @JsonIgnore
    public boolean isFalse() {
        return !Boolean.TRUE.equals(this.data);
    }

    public T getOrDefault(T defaultValue) {
        return data != null ? data : defaultValue;
    }
}
