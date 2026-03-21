package com.example.project_backend04.exception;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerBooking.ConflictCheckResult;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle booking conflict exceptions
     */
    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ApiResponse<ConflictCheckResult>> handleBookingConflict(
            BookingConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.<ConflictCheckResult>builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getConflictDetails())
                .status(HttpStatus.CONFLICT.value())
                .build());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(
            EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(TrainerNotAvailableException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTrainerNotAvailable(
            TrainerNotAvailableException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("trainerId", ex.getTrainerId());
        if (ex.getRequestedDate() != null) {
            details.put("requestedDate", ex.getRequestedDate());
            details.put("requestedStartTime", ex.getRequestedStartTime());
            details.put("requestedEndTime", ex.getRequestedEndTime());
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.CONFLICT.value())
                .build());
    }
    

    @ExceptionHandler(InvalidBookingStatusException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidBookingStatus(
            InvalidBookingStatusException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("bookingId", ex.getBookingId());
        details.put("currentStatus", ex.getCurrentStatus());
        if (ex.getRequiredStatus() != null) {
            details.put("requiredStatus", ex.getRequiredStatus());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(TrainerSpecialtyMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTrainerSpecialtyMismatch(
            TrainerSpecialtyMismatchException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("trainerId", ex.getTrainerId());
        details.put("serviceId", ex.getServiceId());
        details.put("requiredSpecialties", ex.getRequiredSpecialties());
        details.put("trainerSpecialties", ex.getTrainerSpecialties());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotWritableException(
            HttpMessageNotWritableException ex, 
            WebRequest request
    ) {
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
