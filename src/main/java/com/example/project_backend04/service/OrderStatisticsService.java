package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Product.OrderStatisticsResponse;
import com.example.project_backend04.dto.response.Product.TopSellingProductResponse;
import com.example.project_backend04.enums.DeliveryStatus;
import com.example.project_backend04.entity.PaymentStatus;
import com.example.project_backend04.entity.ProductOrder;
import com.example.project_backend04.entity.ProductOrderItem;
import com.example.project_backend04.repository.ProductOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderStatisticsService {
    
    private final ProductOrderRepository productOrderRepository;
    
    /**
     * Get comprehensive order statistics for a date range
     * 
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @return OrderStatisticsResponse with all statistics
     */
    public OrderStatisticsResponse getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalRevenue = productOrderRepository.calculateTotalRevenue(startDate, endDate);

        long totalOrders = productOrderRepository.countOrdersByDateRange(startDate, endDate);
        
        // Count orders by payment status
        long pendingOrders = productOrderRepository.countOrdersByPaymentStatus(
            PaymentStatus.PENDING, startDate, endDate
        );
        long paidOrders = productOrderRepository.countOrdersByPaymentStatus(
            PaymentStatus.PAID, startDate, endDate
        );
        long cancelledOrders = productOrderRepository.countOrdersByPaymentStatus(
            PaymentStatus.CANCELLED, startDate, endDate
        );
        
        // Count orders by delivery status
        long pendingDeliveries = productOrderRepository.countOrdersByDeliveryStatus(
            DeliveryStatus.PENDING, startDate, endDate
        );
        long processingDeliveries = productOrderRepository.countOrdersByDeliveryStatus(
            DeliveryStatus.PROCESSING, startDate, endDate
        );
        long shippedDeliveries = productOrderRepository.countOrdersByDeliveryStatus(
            DeliveryStatus.SHIPPED, startDate, endDate
        );
        long deliveredOrders = productOrderRepository.countOrdersByDeliveryStatus(
            DeliveryStatus.DELIVERED, startDate, endDate
        );
        
        return OrderStatisticsResponse.builder()
            .totalRevenue(totalRevenue)
            .totalOrders((int) totalOrders)
            .pendingOrders((int) pendingOrders)
            .paidOrders((int) paidOrders)
            .cancelledOrders((int) cancelledOrders)
            .pendingDeliveries((int) pendingDeliveries)
            .processingDeliveries((int) processingDeliveries)
            .shippedDeliveries((int) shippedDeliveries)
            .deliveredOrders((int) deliveredOrders)
            .build();
    }
    
    /**
     * Get top-selling products ranked by total quantity sold
     * 
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @param limit Maximum number of products to return
     * @return List of TopSellingProductResponse sorted by quantity descending
     */
    public List<TopSellingProductResponse> getTopSellingProducts(
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        int limit
    ) {
        // Get all PAID orders in date range
        List<ProductOrder> paidOrders = productOrderRepository.findPaidOrdersByDateRange(
            startDate, endDate
        );
        
        // Map to aggregate product sales: productId -> {totalQuantity, totalRevenue, productInfo}
        Map<Long, ProductSalesData> productSalesMap = new HashMap<>();
        
        for (ProductOrder order : paidOrders) {
            for (ProductOrderItem item : order.getItems()) {
                Long productId = item.getProduct().getId();
                
                ProductSalesData salesData = productSalesMap.computeIfAbsent(
                    productId, 
                    k -> new ProductSalesData(
                        productId,
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl()
                    )
                );
                
                salesData.addSale(item.getQuantity(), item.getSubtotal());
            }
        }
        
        // Convert to response DTOs and sort by quantity descending
        return productSalesMap.values().stream()
            .map(data -> TopSellingProductResponse.builder()
                .productId(data.productId)
                .productName(data.productName)
                .productImageUrl(data.productImageUrl)
                .totalQuantitySold(data.totalQuantity)
                .totalRevenue(data.totalRevenue)
                .build())
            .sorted((a, b) -> Integer.compare(b.getTotalQuantitySold(), a.getTotalQuantitySold()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Helper class to aggregate product sales data
     */
    private static class ProductSalesData {
        private final Long productId;
        private final String productName;
        private final String productImageUrl;
        private int totalQuantity;
        private BigDecimal totalRevenue;
        
        public ProductSalesData(Long productId, String productName, String productImageUrl) {
            this.productId = productId;
            this.productName = productName;
            this.productImageUrl = productImageUrl;
            this.totalQuantity = 0;
            this.totalRevenue = BigDecimal.ZERO;
        }
        
        public void addSale(int quantity, BigDecimal revenue) {
            this.totalQuantity += quantity;
            this.totalRevenue = this.totalRevenue.add(revenue);
        }
    }
}
