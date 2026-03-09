package com.example.project_backend04.exception;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotWritableException(
            HttpMessageNotWritableException ex, 
            WebRequest request
    ) {
        System.err.println("=== HttpMessageNotWritableException ===");
        System.err.println("Message: " + ex.getMessage());
        System.err.println("Cause: " + (ex.getCause() != null ? ex.getCause().getMessage() : "null"));
        ex.printStackTrace();
        
        String errorMessage = "Failed to serialize response: " + ex.getMessage();
        if (ex.getCause() != null) {
            errorMessage += " | Cause: " + ex.getCause().getMessage();
        }
        
        ApiResponse<String> response = new ApiResponse<>(
            false,
            errorMessage,
            null,
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGlobalException(
            Exception ex, 
            WebRequest request
    ) {
        System.err.println("=== Global Exception Handler ===");
        System.err.println("Exception type: " + ex.getClass().getName());
        System.err.println("Message: " + ex.getMessage());
        ex.printStackTrace();
        
        ApiResponse<String> response = new ApiResponse<>(
            false,
            "Internal server error: " + ex.getMessage(),
            null,
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
