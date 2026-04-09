package com.example.project_backend04.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatisticsResponse {
    private BigDecimal totalRevenue;
    private int totalOrders;
    private int pendingOrders;
    private int paidOrders;
    private int cancelledOrders;
    private int pendingDeliveries;
    private int processingDeliveries;
    private int shippedDeliveries;
    private int deliveredOrders;
}
