package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.IService.IServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final IServiceCategoryService serviceCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getAllActiveServiceCategories() {
        ApiResponse<List<ServiceCategoryResponse>> response = serviceCategoryService.getAllActiveServiceCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> getServiceCategoryByName(@PathVariable String name) {
        ApiResponse<ServiceCategoryResponse> response = serviceCategoryService.getServiceCategoryByName(name);
        return ResponseEntity
                .status(response.isSuccess() ? 200 : 404)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> getServiceCategoryById(@PathVariable Long id) {
        ApiResponse<ServiceCategoryResponse> response = serviceCategoryService.getServiceCategoryById(id);
        return ResponseEntity
                .status(response.isSuccess() ? 200 : 404)
                .body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> searchServiceCategories(
            @RequestParam String keyword) {
        
        ApiResponse<List<ServiceCategoryResponse>> response = serviceCategoryService.searchServiceCategories(keyword);
        return ResponseEntity.ok(response);
    }
}