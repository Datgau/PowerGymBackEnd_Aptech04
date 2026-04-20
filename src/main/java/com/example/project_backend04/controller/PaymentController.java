package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Payment.CreatePaymentRequest;
import com.example.project_backend04.dto.request.Payment.MoMoIPNRequest;
import com.example.project_backend04.dto.response.Payment.MoMoPaymentResponse;
import com.example.project_backend04.dto.response.Payment.PaymentOrderResponse;
import com.example.project_backend04.dto.response.Payment.PaymentWithTrainerSelectionResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.mapper.PaymentOrderMapper;
import com.example.project_backend04.service.MoMoPaymentService;
import com.example.project_backend04.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MoMoPaymentService moMoPaymentService;
    private final PaymentOrderMapper paymentOrderMapper;

    @PostMapping("/momo/create")
    public ResponseEntity<ApiResponse<MoMoPaymentResponse>> createMoMoPayment(
            @RequestBody CreatePaymentRequest request) {
        try {
            User user = SecurityUtils.getCurrentUser();
            
            log.info("Creating MoMo payment - User: {}, Amount: {}", 
                user != null ? user.getFullName() : "NULL", request.getAmount());
            
            if (user == null) {
                log.error("User is null in createMoMoPayment endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User authentication required"));
            }
            
            MoMoPaymentResponse response = moMoPaymentService.createPayment(request, user);
            return ResponseEntity.ok(ApiResponse.success(response, "Payment created successfully"));
        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create payment: " + e.getMessage()));
        }
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<String> handleMoMoIPN(@RequestBody MoMoIPNRequest ipnRequest) {
        try {
            log.info("Received MoMo IPN: {}", ipnRequest.getOrderId());
            moMoPaymentService.handleIPN(ipnRequest);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error handling MoMo IPN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    @GetMapping("/status/{orderId}/with-trainer-selection")
    public ResponseEntity<ApiResponse<PaymentWithTrainerSelectionResponse>> getPaymentStatusWithTrainerSelection(
            @PathVariable String orderId) {
        try {
            User user = SecurityUtils.getCurrentUser();

            if (user == null) {
                log.error("User is null in getPaymentStatusWithTrainerSelection endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User authentication required"));
            }
            
            PaymentOrder paymentOrder = moMoPaymentService.getPaymentStatus(orderId);
            if (paymentOrder.getUser() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Payment order has no associated user"));
            }
            
            if (!paymentOrder.getUser().getId().equals(user.getId())) {
                log.warn("Access denied: PaymentOrder {} belongs to user {} but requested by user {}",
                    orderId, paymentOrder.getUser().getId(), user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
            
            PaymentWithTrainerSelectionResponse response = 
                moMoPaymentService.getPaymentWithTrainerSelection(paymentOrder);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting payment status with trainer selection for orderId: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Payment not found: " + e.getMessage()));
        }
    }

    @GetMapping("/debug/status/{orderId}")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getPaymentStatusDebug(
            @PathVariable String orderId) {
        try {
            log.info("DEBUG: Getting payment status for orderId: {}", orderId);
            PaymentOrder paymentOrder = moMoPaymentService.getPaymentStatus(orderId);
            
            log.info("DEBUG: PaymentOrder found - ID: {}, User: {}, Status: {}", 
                paymentOrder.getId(), 
                paymentOrder.getUser() != null ? paymentOrder.getUser().getId() : "NULL",
                paymentOrder.getStatus());
            
            PaymentOrderResponse response = paymentOrderMapper.toResponse(paymentOrder);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("DEBUG: Error getting payment status for orderId: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Payment not found: " + e.getMessage()));
        }
    }

    /**
     * Get user's payment history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentOrderResponse>>> getPaymentHistory() {
        try {
            User user = SecurityUtils.getCurrentUser();
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User authentication required"));
            }
            
            List<PaymentOrder> payments = moMoPaymentService.getUserPaymentHistory(user);
            List<PaymentOrderResponse> responses = payments.stream()
                    .map(paymentOrderMapper::toResponse)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses, "Payment history retrieved"));
        } catch (Exception e) {
            log.error("Error getting payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get payment history: " + e.getMessage()));
        }
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<ApiResponse<String>> cancelPayment(@PathVariable String orderId) {
        try {
            User user = SecurityUtils.getCurrentUser();
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User authentication required"));
            }
            return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cancel payment: " + e.getMessage()));
        }
    }
}