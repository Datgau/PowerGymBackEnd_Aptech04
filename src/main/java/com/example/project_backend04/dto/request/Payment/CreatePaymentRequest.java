package com.example.project_backend04.dto.request.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    private Long amount;
    private String orderInfo;
    private String extraData;
    private String lang;
    
    // Item information for linking payment to service/membership
    private String itemType; // "SERVICE" or "MEMBERSHIP"
    private String itemId;   // Service ID or Package ID
    private String itemName; // Service name or Package name
}