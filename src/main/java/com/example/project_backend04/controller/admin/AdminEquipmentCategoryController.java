package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.EquipmentCategory.CreateEquipmentCategoryDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.EquipmentCategory;
import com.example.project_backend04.service.IService.IEquipmentCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/equipment-categories")
@RequiredArgsConstructor
public class AdminEquipmentCategoryController {
    
    private final IEquipmentCategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EquipmentCategory>>> getAllCategories() {
        try {
            ApiResponse<List<EquipmentCategory>> response = categoryService.getAllCategories();
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<EquipmentCategory>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<EquipmentCategory>>> getActiveCategories() {
        try {
            ApiResponse<List<EquipmentCategory>> response = categoryService.getActiveCategories();
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<EquipmentCategory>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipmentCategory>> getCategoryById(@PathVariable Long id) {
        ApiResponse<EquipmentCategory> response = categoryService.getCategoryById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<EquipmentCategory>> createCategory(
            @Valid @RequestBody CreateEquipmentCategoryDto request
    ) {
        ApiResponse<EquipmentCategory> response = categoryService.createCategory(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipmentCategory>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateEquipmentCategoryDto request
    ) {
        ApiResponse<EquipmentCategory> response = categoryService.updateCategory(id, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        ApiResponse<Void> response = categoryService.deleteCategory(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}