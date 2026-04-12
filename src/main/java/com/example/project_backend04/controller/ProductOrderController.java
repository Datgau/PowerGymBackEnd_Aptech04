package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.CreateProductOrderRequest;
import com.example.project_backend04.dto.request.CreateOrderFromPaymentRequest;
import com.example.project_backend04.dto.request.UpdateDeliveryStatusRequest;
import com.example.project_backend04.dto.request.UpdatePaymentStatusRequest;
import com.example.project_backend04.dto.response.Product.OrderStatisticsResponse;
import com.example.project_backend04.dto.response.Product.ProductOrderDetailResponse;
import com.example.project_backend04.dto.response.Product.ProductOrderResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.enums.DeliveryStatus;
import com.example.project_backend04.entity.PaymentStatus;
import com.example.project_backend04.enums.SaleType;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.service.InvoicePrintService;
import com.example.project_backend04.service.OrderStatisticsService;
import com.example.project_backend04.service.ProductOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Product Order Management
 * Handles order operations with role-based access control
 */
@RestController
@RequestMapping("/api/product-orders")
@RequiredArgsConstructor
@Slf4j
public class ProductOrderController {
    
    private final ProductOrderService productOrderService;
    private final OrderStatisticsService orderStatisticsService;
    private final InvoicePrintService invoicePrintService;
    

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductOrderResponse>>> getAllProductOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) SaleType saleType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }

        User user = null;
        if (userDetails instanceof com.example.project_backend04.security.CustomUserDetails) {
            user = ((com.example.project_backend04.security.CustomUserDetails) userDetails).getUser();
        }
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }

        Page<ProductOrderResponse> orders = productOrderService.getAllProductOrders(
                page, size, paymentStatus, deliveryStatus, saleType, 
                startDate, endDate, search, user
        );
        return ResponseEntity.ok(ApiResponse.success(orders, "Product orders retrieved successfully"));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderStatisticsResponse>> getOrderStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate endDate
    ) {
        LocalDateTime effectiveStartDate = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEndDate = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        OrderStatisticsResponse statistics = orderStatisticsService.getOrderStatistics(
                effectiveStartDate, effectiveEndDate
        );
        return ResponseEntity.ok(ApiResponse.success(statistics, "Order statistics retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductOrderDetailResponse>> getProductOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("GET /api/product-orders/{} - UserDetails is null, authentication failed", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        // Extract User from CustomUserDetails
        User user = null;
        if (userDetails instanceof com.example.project_backend04.security.CustomUserDetails) {
            user = ((com.example.project_backend04.security.CustomUserDetails) userDetails).getUser();
        }
        
        if (user == null) {
            log.error("GET /api/product-orders/{} - Could not extract User from UserDetails", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        log.info("GET /api/product-orders/{} - user: {}", id, user.getId());
        
        ProductOrderDetailResponse order = productOrderService.getProductOrderById(id, user);
        return ResponseEntity.ok(ApiResponse.success(order, "Product order retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductOrderResponse>> createProductOrder(
            @Valid @RequestBody CreateProductOrderRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("POST /api/product-orders - UserDetails is null, authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        // Extract User from CustomUserDetails
        User user = null;
        if (userDetails instanceof com.example.project_backend04.security.CustomUserDetails) {
            user = ((com.example.project_backend04.security.CustomUserDetails) userDetails).getUser();
        }
        
        if (user == null) {
            log.error("POST /api/product-orders - Could not extract User from UserDetails");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        log.info("POST /api/product-orders - Creating order for user: {}", user.getId());
        
        ProductOrderResponse order = productOrderService.createProductOrder(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Product order created successfully"));
    }
    
    /**
     * Create product order from successful payment
     * This endpoint is called after payment succeeds to create the actual order
     * Access: Authenticated users only
     * 
     * @param request Order creation request with payment ID and delivery info
     * @param userDetails Authenticated user details
     * @return Created product order
     */
    @PostMapping("/from-payment")
    public ResponseEntity<ApiResponse<ProductOrderResponse>> createOrderFromPayment(
            @Valid @RequestBody CreateOrderFromPaymentRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("POST /api/product-orders/from-payment - UserDetails is null, authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        // Extract User from CustomUserDetails
        User user = null;
        if (userDetails instanceof com.example.project_backend04.security.CustomUserDetails) {
            user = ((com.example.project_backend04.security.CustomUserDetails) userDetails).getUser();
        }
        
        if (user == null) {
            log.error("POST /api/product-orders/from-payment - Could not extract User from UserDetails");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        log.info("POST /api/product-orders/from-payment - Creating order from payment: {} for user: {}", 
                request.getPaymentId(), user.getId());
        
        ProductOrderResponse order = productOrderService.createProductOrderFromPayment(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Product order created from payment successfully"));
    }
    
    /**
     * Update payment status of a product order
     * Access: ADMIN only
     * 
     * @param id Product order ID
     * @param request Payment status update request
     * @return Updated product order
     */
    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductOrderResponse>> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        log.info("PUT /api/product-orders/{}/payment-status - Updating to: {}", id, request.getPaymentStatus());
        
        ProductOrderResponse order = productOrderService.updatePaymentStatus(id, request.getPaymentStatus());
        return ResponseEntity.ok(ApiResponse.success(order, "Payment status updated successfully"));
    }
    
    /**
     * Update delivery status of a product order
     * Access: ADMIN only
     * 
     * @param id Product order ID
     * @param request Delivery status update request
     * @return Updated product order
     */
    @PutMapping("/{id}/delivery-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductOrderResponse>> updateDeliveryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeliveryStatusRequest request
    ) {
        log.info("PUT /api/product-orders/{}/delivery-status - Updating to: {}", id, request.getDeliveryStatus());
        
        ProductOrderResponse order = productOrderService.updateDeliveryStatus(id, request.getDeliveryStatus());
        return ResponseEntity.ok(ApiResponse.success(order, "Delivery status updated successfully"));
    }
    
    /**
     * Generate and download product order invoice as PDF
     * Access: Authenticated users (can only access their own orders, admins can access all)
     * 
     * @param id Product order ID
     * @param userDetails Authenticated user details
     * @return PDF file as byte array
     */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<?> downloadInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("GET /api/product-orders/{}/invoice - UserDetails is null, authentication failed", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        // Extract User from CustomUserDetails
        User user = null;
        if (userDetails instanceof com.example.project_backend04.security.CustomUserDetails) {
            user = ((com.example.project_backend04.security.CustomUserDetails) userDetails).getUser();
        }
        
        if (user == null) {
            log.error("GET /api/product-orders/{}/invoice - Could not extract User from UserDetails", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        log.info("GET /api/product-orders/{}/invoice - Generating invoice for user: {}", id, user.getId());
        
        try {
            // Get order details (this also checks access permissions)
            ProductOrderDetailResponse order = productOrderService.getProductOrderById(id, user);
            
            // Generate PDF
            byte[] pdfBytes = invoicePrintService.generateProductOrderInvoicePdf(order);
            
            // Set headers for PDF download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice-order-" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Error generating invoice for order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate invoice: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
