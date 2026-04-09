package com.example.project_backend04.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderFromPaymentRequest {
    @NotBlank(message = "Payment ID is required")
    private String paymentId;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name cannot exceed 255 characters")
    private String customerName;
    
    @NotBlank(message = "Customer phone is required")
    @Size(max = 50, message = "Customer phone cannot exceed 50 characters")
    private String customerPhone;
    
    @NotBlank(message = "Customer address is required")
    @Size(max = 500, message = "Customer address cannot exceed 500 characters")
    private String customerAddress;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    @NotEmpty(message = "Cart items list cannot be empty")
    @Valid
    private List<CartItemRequest> cartItems;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemRequest {
        private Long productId;
        private Integer quantity;
    }
}
