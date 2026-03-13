package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.EquipmentCategory.CreateEquipmentCategoryDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.EquipmentCategory;

import java.util.List;

public interface IEquipmentCategoryService {
    ApiResponse<List<EquipmentCategory>> getAllCategories();
    ApiResponse<List<EquipmentCategory>> getActiveCategories();
    ApiResponse<EquipmentCategory> getCategoryById(Long id);
    ApiResponse<EquipmentCategory> createCategory(CreateEquipmentCategoryDto request);
    ApiResponse<EquipmentCategory> updateCategory(Long id, CreateEquipmentCategoryDto request);
    ApiResponse<Void> deleteCategory(Long id);
}