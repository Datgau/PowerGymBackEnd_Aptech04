package com.example.project_backend04.dto.response.Product;

import com.example.project_backend04.enums.DeliveryStatus;
import com.example.project_backend04.entity.PaymentStatus;
import com.example.project_backend04.enums.SaleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOrderDetailResponse {
    private Long id;
    private Long userId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private SaleType saleType;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private DeliveryStatus deliveryStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductOrderItemResponse> items;
}
