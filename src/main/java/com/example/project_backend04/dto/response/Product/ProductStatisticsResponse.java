package com.example.project_backend04.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatisticsResponse {
    private int totalProducts;
    private int inStockProducts;
    private int lowStockProducts;
    private int outOfStockProducts;
    private List<ProductResponse> lowStockProductList;
    private List<TopSellingProductResponse> topSellingProducts;
}
