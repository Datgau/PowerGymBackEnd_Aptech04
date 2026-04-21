package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.EquipmentCategory.CreateEquipmentCategoryDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.EquipmentCategory;
import com.example.project_backend04.mapper.EquipmentCategoryMapper;
import com.example.project_backend04.repository.EquipmentCategoryRepository;
import com.example.project_backend04.service.IService.IEquipmentCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EquipmentCategoryService implements IEquipmentCategoryService {
    
    private final EquipmentCategoryRepository categoryRepository;
    private final EquipmentCategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<EquipmentCategory>> getAllCategories() {
        try {
            List<EquipmentCategory> categories = categoryRepository.findAll();
            return ApiResponse.success(categories, "Categories retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<EquipmentCategory>> getActiveCategories() {
        try {
            List<EquipmentCategory> categories = categoryRepository.findByIsActiveTrue();
            return ApiResponse.success(categories, "Active categories retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EquipmentCategory> getCategoryById(Long id) {
        Optional<EquipmentCategory> categoryOpt = categoryRepository.findById(id);
        
        if (categoryOpt.isEmpty()) {
            return ApiResponse.error("Category not found");
        }
        return ApiResponse.success(categoryOpt.get(), "Category retrieved successfully");
    }

    @Override
    @Transactional
    public ApiResponse<EquipmentCategory> createCategory(CreateEquipmentCategoryDto request) {
        try {
            if (categoryRepository.existsByName(request.getName())) {
                return ApiResponse.error("Category name already exists");
            }

            EquipmentCategory category = categoryMapper.toEntity(request);
            EquipmentCategory savedCategory = categoryRepository.save(category);
            return ApiResponse.success(savedCategory, "Category created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to create category: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<EquipmentCategory> updateCategory(Long id, CreateEquipmentCategoryDto request) {
        Optional<EquipmentCategory> existingCategoryOpt = categoryRepository.findById(id);
        
        if (existingCategoryOpt.isEmpty()) {
            return ApiResponse.error("Category not found");
        }

        try {
            EquipmentCategory existingCategory = existingCategoryOpt.get();
            if (!existingCategory.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
                return ApiResponse.error("Category name already exists");
            }

            categoryMapper.updateEntityFromDto(request, existingCategory);
            EquipmentCategory updatedCategory = categoryRepository.save(existingCategory);
            return ApiResponse.success(updatedCategory, "Category updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to update category: " + e.getMessage());
        }
    }
    @Override
    @Transactional
    public ApiResponse<Void> deleteCategory(Long id) {
        Optional<EquipmentCategory> categoryOpt = categoryRepository.findById(id);
        
        if (categoryOpt.isEmpty()) {
            return ApiResponse.error("Category not found");
        }
        
        try {
            categoryRepository.deleteById(id);
            return ApiResponse.success(null, "Category deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to delete category: " + e.getMessage());
        }
    }
}