package com.example.project_backend04.exception;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerBooking.ConflictCheckResult;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
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
            EntityNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("ENTITY_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("INVALID_ARGUMENT")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("ILLEGAL_STATE")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
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
            MethodArgumentNotValidException ex, HttpServletRequest request) {
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
                .errorCode("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
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
        
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(errorMessage)
                .data(null)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("SERIALIZATION_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle bank payment exceptions
     */
    @ExceptionHandler(BankPaymentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBankPaymentException(
            BankPaymentException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex instanceof PaymentOrderNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            details.put("content", ((PaymentOrderNotFoundException) ex).getContent());
        } else if (ex instanceof ServiceNotActiveException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            details.put("serviceId", ((ServiceNotActiveException) ex).getServiceId());
        } else if (ex instanceof DatabaseRetryException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            DatabaseRetryException dbEx = (DatabaseRetryException) ex;
            details.put("maxRetries", dbEx.getMaxRetries());
            details.put("attemptsMade", dbEx.getAttemptsMade());
        }
        
        return ResponseEntity.status(status)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(status.value())
                .errorCode(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle database retry exceptions
     */
    @ExceptionHandler(DatabaseRetryException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDatabaseRetryException(
            DatabaseRetryException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", "DATABASE_RETRY_FAILED");
        details.put("maxRetries", ex.getMaxRetries());
        details.put("attemptsMade", ex.getAttemptsMade());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Database operation failed after " + ex.getMaxRetries() + " retry attempts")
                .data(details)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("DATABASE_RETRY_FAILED")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle payment order not found exceptions
     */
    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePaymentOrderNotFoundException(
            PaymentOrderNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", "PAYMENT_ORDER_NOT_FOUND");
        details.put("content", ex.getContent());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("PAYMENT_ORDER_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle service not active exceptions
     */
    @ExceptionHandler(ServiceNotActiveException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleServiceNotActiveException(
            ServiceNotActiveException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", "SERVICE_NOT_ACTIVE");
        details.put("serviceId", ex.getServiceId());
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .errorCode("SERVICE_NOT_ACTIVE")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle data access exceptions (database errors)
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", "DATABASE_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Database operation failed: " + ex.getMessage())
                .data(details)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("DATABASE_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle trainer not found exceptions
     */
    @ExceptionHandler(TrainerNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTrainerNotFoundException(
            TrainerNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("trainerId", ex.getTrainerId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("TRAINER_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle invalid role exceptions
     */
    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidRoleException(
            InvalidRoleException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("userId", ex.getUserId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("INVALID_ROLE")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle invalid percentage exceptions
     */
    @ExceptionHandler(InvalidPercentageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPercentageException(
            InvalidPercentageException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("INVALID_PERCENTAGE")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle product not found exceptions
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleProductNotFoundException(
            ProductNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("productId", ex.getProductId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("PRODUCT_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle product order not found exceptions
     */
    @ExceptionHandler(ProductOrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleProductOrderNotFoundException(
            ProductOrderNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", ex.getOrderId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("ORDER_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle insufficient stock exceptions
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInsufficientStockException(
            InsufficientStockException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("productId", ex.getProductId());
        details.put("productName", ex.getProductName());
        details.put("requestedQuantity", ex.getRequestedQuantity());
        details.put("availableStock", ex.getAvailableStock());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.CONFLICT.value())
                .errorCode("INSUFFICIENT_STOCK")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle invalid status transition exceptions
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidStatusTransitionException(
            InvalidStatusTransitionException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("currentStatus", ex.getCurrentStatus());
        details.put("requestedStatus", ex.getRequestedStatus());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.CONFLICT.value())
                .errorCode("INVALID_STATUS_TRANSITION")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Handle import receipt not found exceptions
     */
    @ExceptionHandler(ImportReceiptNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleImportReceiptNotFoundException(
            ImportReceiptNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("importReceiptId", ex.getImportReceiptId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .data(details)
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("IMPORT_RECEIPT_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGlobalException(
            Exception ex, 
            HttpServletRequest request
    ) {
        ex.printStackTrace();
        
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message("Internal server error: " + ex.getMessage())
                .data(null)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
