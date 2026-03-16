package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.ServiceCategory.CreateServiceCategoryRequest;
import com.example.project_backend04.dto.request.ServiceCategory.UpdateServiceCategoryRequest;
import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IServiceCategoryService {
    
    // CRUD operations
    ApiResponse<ServiceCategoryResponse> createServiceCategory(CreateServiceCategoryRequest request);
    ApiResponse<ServiceCategoryResponse> updateServiceCategory(UpdateServiceCategoryRequest request);
    ApiResponse<String> deleteServiceCategory(Long id);
    
    // Get operations
    ApiResponse<ServiceCategoryResponse> getServiceCategoryById(Long id);
    ApiResponse<ServiceCategoryResponse> getServiceCategoryByName(String name);
    ApiResponse<List<ServiceCategoryResponse>> getAllServiceCategories();
    ApiResponse<List<ServiceCategoryResponse>> getAllActiveServiceCategories();
    ApiResponse<Page<ServiceCategoryResponse>> getAllServiceCategories(int page, int size);
    
    // Search operations
    ApiResponse<List<ServiceCategoryResponse>> searchServiceCategories(String keyword);
    
    // Statistics
    ApiResponse<List<ServiceCategoryResponse>> getCategoriesUsedByTrainers();
    ApiResponse<List<ServiceCategoryResponse>> getCategoriesUsedByGymServices();
    
    // Utility operations
    ApiResponse<String> reorderServiceCategories(List<Long> categoryIds);
    ApiResponse<String> toggleServiceCategoryStatus(Long id);
}