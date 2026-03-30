package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.BankPayment.CreateBankPaymentRequest;
import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;
import com.example.project_backend04.dto.response.BankPayment.CreateBankPaymentResponse;
import com.example.project_backend04.dto.response.BankPayment.PaymentStatusResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.exception.BankPaymentException;
import com.example.project_backend04.service.BankPaymentService;
import com.example.project_backend04.config.BankPaymentConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank-payments")
@RequiredArgsConstructor
@Slf4j
public class BankPaymentController {

    private final BankPaymentService bankPaymentService;
    private final BankPaymentConfig bankPaymentConfig;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreateBankPaymentResponse>> createBankPayment(
            @RequestBody @Valid CreateBankPaymentRequest request) {
        log.info("AUDIT_LOG - API_REQUEST - Endpoint: /api/bank-payments/create, UserId: {}, ServiceId: {}, Timestamp: {}",
            request.getUserId(), request.getServiceId(), java.time.LocalDateTime.now());
        
        try {
            CreateBankPaymentResponse response = bankPaymentService.createBankPayment(
                request.getUserId(), request.getServiceId());
            return ResponseEntity.ok(ApiResponse.success(response, "Bank payment created successfully"));
            
        } catch (Exception e) {
            log.error("AUDIT_LOG - API_RESPONSE - Endpoint: /api/bank-payments/create, UserId: {}, ServiceId: {}, Result: FAILURE, Error: {}, Timestamp: {}",
                request.getUserId(), request.getServiceId(), e.getMessage(), java.time.LocalDateTime.now());
            throw e;
        }
    }
    @PostMapping("/webhook")
    public ResponseEntity<String> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Valid SepayWebhookRequest request) {

        try {

            log.info("AUDIT_LOG - WEBHOOK_API_REQUEST - Endpoint: /api/bank-payments/webhook, Description: {}, Amount: {}, TransactionId: {}, Timestamp: {}",
                    request.getDescription(), request.getAmount(), request.getTransactionId(), java.time.LocalDateTime.now());

            // ===== Validate Authorization header =====
            if (authorization == null || !authorization.startsWith("Apikey ")) {
                log.warn("AUDIT_LOG - WEBHOOK_API_RESPONSE - Result: INVALID_AUTH_HEADER, Timestamp: {}",
                        java.time.LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            // ===== Extract API key =====
            String apiKey = authorization.replace("Apikey ", "").trim();

            // ===== Compare API key =====
            if (!bankPaymentConfig.sepayApiKey().equals(apiKey)) {
                log.warn("AUDIT_LOG - WEBHOOK_API_RESPONSE - Result: INVALID_API_KEY, Timestamp: {}",
                        java.time.LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            // ===== Process webhook =====
            bankPaymentService.handleSepayWebhook(request);

            // ===== Success log =====
            log.info("AUDIT_LOG - WEBHOOK_API_RESPONSE - Description: {}, Result: SUCCESS, Timestamp: {}",
                    request.getDescription(), java.time.LocalDateTime.now());

            return ResponseEntity.ok("OK");

        } catch (BankPaymentException e) {

            log.error("AUDIT_LOG - WEBHOOK_API_RESPONSE - Description: {}, Result: VALIDATION_ERROR, Error: {}, ErrorCode: {}, Timestamp: {}",
                    request != null ? request.getDescription() : "UNKNOWN",
                    e.getMessage(), e.getErrorCode(), java.time.LocalDateTime.now());

            if ("VALIDATION_ERROR".equals(e.getErrorCode()) ||
                    "INVALID_PAYLOAD".equals(e.getErrorCode()) ||
                    "INVALID_DESCRIPTION".equals(e.getErrorCode()) ||
                    "INVALID_AMOUNT".equals(e.getErrorCode()) ||
                    "SECURITY_VIOLATION".equals(e.getErrorCode())) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Bad Request: " + e.getMessage());
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {

            log.error("AUDIT_LOG - WEBHOOK_API_RESPONSE - Description: {}, Result: FAILURE, Error: {}, Timestamp: {}",
                    request != null ? request.getDescription() : "UNKNOWN",
                    e.getMessage(), java.time.LocalDateTime.now());

            return ResponseEntity.ok("OK");
        }
    }
    /**
     * Get payment status by content identifier
     * Requirements: 4.3, 4.4, 4.6
     */
    @GetMapping("/status/{content}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String content) {
        // Audit log: Status API request received
        log.info("AUDIT_LOG - STATUS_API_REQUEST - Endpoint: /api/bank-payments/status/{}, Content: {}, Timestamp: {}", 
            content, content, java.time.LocalDateTime.now());
        
        try {
            PaymentStatusResponse response = bankPaymentService.checkPaymentStatus(content);
            
            // Audit log: Status API response success
            log.info("AUDIT_LOG - STATUS_API_RESPONSE - Endpoint: /api/bank-payments/status/{}, Content: {}, Status: {}, OrderId: {}, Result: SUCCESS, Timestamp: {}", 
                content, content, response.getStatus(), response.getOrderId(), java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response, "Payment status retrieved successfully"));
            
        } catch (Exception e) {
            // Audit log: Status API response failure
            log.error("AUDIT_LOG - STATUS_API_RESPONSE - Endpoint: /api/bank-payments/status/{}, Content: {}, Result: FAILURE, Error: {}, Timestamp: {}", 
                content, content, e.getMessage(), java.time.LocalDateTime.now());
            throw e; // Re-throw to be handled by GlobalExceptionHandler
        }
    }
}