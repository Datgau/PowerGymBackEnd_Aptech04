package com.example.project_backend04.dto.request.BankPayment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBankPaymentRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    private Long serviceId;
    
    private Long packageId;
    
    private String itemType;
    
    private Long bookingId;
    
    private String promotionCode; // Promotion code for discount
    
    private Long amount; // Override amount (for products or discounted price)
    
    private String itemName; // Item name (for products)
}