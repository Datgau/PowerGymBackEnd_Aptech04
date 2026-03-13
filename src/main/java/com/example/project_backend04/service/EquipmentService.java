package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Equipment.CreateEquipmentDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.Equipment;
import com.example.project_backend04.entity.EquipmentCategory;
import com.example.project_backend04.mapper.EquipmentMapper;
import com.example.project_backend04.repository.EquipmentCategoryRepository;
import com.example.project_backend04.repository.EquipmentRepository;
import com.example.project_backend04.service.IService.IEquipmentService;
import com.example.project_backend04.service.IService.ICloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EquipmentService implements IEquipmentService {
    
    private final EquipmentRepository equipmentRepository;
    private final EquipmentCategoryRepository categoryRepository;
    private final EquipmentMapper equipmentMapper;
    private final ICloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<Equipment>> getAllEquipments() {
        try {
            List<Equipment> equipments = equipmentRepository.findAll();
            return ApiResponse.success(equipments, "Equipments retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<Equipment>> getActiveEquipments() {
        try {
            List<Equipment> equipments = equipmentRepository.findByIsActiveTrue();
            return ApiResponse.success(equipments, "Active equipments retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<Equipment>> getEquipmentsByCategory(Long categoryId) {
        try {
            List<Equipment> equipments = equipmentRepository.findByCategoryId(categoryId);
            return ApiResponse.success(equipments, "Equipments by category retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<Equipment>> getActiveEquipmentsByCategory(Long categoryId) {
        try {
            List<Equipment> equipments = equipmentRepository.findByCategoryIdAndIsActiveTrue(categoryId);
            return ApiResponse.success(equipments, "Active equipments by category retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Equipment> getEquipmentById(Long id) {
        Optional<Equipment> equipmentOpt = equipmentRepository.findById(id);
        
        if (equipmentOpt.isEmpty()) {
            return ApiResponse.error("Equipment not found");
        }
        return ApiResponse.success(equipmentOpt.get(), "Equipment retrieved successfully");
    }

    @Override
    @Transactional
    public ApiResponse<Equipment> createEquipment(CreateEquipmentDto request) {
        try {
            // Validate category exists
            Optional<EquipmentCategory> categoryOpt = categoryRepository.findById(request.getCategoryId());
            if (categoryOpt.isEmpty()) {
                return ApiResponse.error("Category not found");
            }

            Equipment equipment = equipmentMapper.toEntity(request);
            equipment.setCategory(categoryOpt.get());
            
            Equipment savedEquipment = equipmentRepository.save(equipment);
            return ApiResponse.success(savedEquipment, "Equipment created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to create equipment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<Equipment> updateEquipment(Long id, CreateEquipmentDto request) {
        Optional<Equipment> existingEquipmentOpt = equipmentRepository.findById(id);
        
        if (existingEquipmentOpt.isEmpty()) {
            return ApiResponse.error("Equipment not found");
        }

        try {
            // Validate category exists
            Optional<EquipmentCategory> categoryOpt = categoryRepository.findById(request.getCategoryId());
            if (categoryOpt.isEmpty()) {
                return ApiResponse.error("Category not found");
            }

            Equipment existingEquipment = existingEquipmentOpt.get();
            String oldImageUrl = existingEquipment.getImage();
            
            equipmentMapper.updateEntityFromDto(request, existingEquipment);
            existingEquipment.setCategory(categoryOpt.get());
            
            Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
            
            // Delete old image from Cloudinary if it was changed and not empty
            if (oldImageUrl != null && !oldImageUrl.isEmpty() && 
                !oldImageUrl.equals(request.getImage())) {
                try {
                    cloudinaryService.deleteFile(oldImageUrl);
                } catch (Exception e) {
                    System.err.println("Failed to delete old image: " + e.getMessage());
                    // Don't fail the update if image deletion fails
                }
            }
            
            return ApiResponse.success(updatedEquipment, "Equipment updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to update equipment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteEquipment(Long id) {
        Optional<Equipment> equipmentOpt = equipmentRepository.findById(id);
        
        if (equipmentOpt.isEmpty()) {
            return ApiResponse.error("Equipment not found");
        }
        
        try {
            Equipment equipment = equipmentOpt.get();
            String imageUrl = equipment.getImage();
            
            // Delete equipment from database first
            equipmentRepository.deleteById(id);
            
            // Delete image from Cloudinary if exists
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(imageUrl);
                } catch (Exception e) {
                    System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
                    // Don't fail the deletion if image deletion fails
                }
            }
            
            return ApiResponse.success(null, "Equipment deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to delete equipment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<Equipment> createEquipmentWithImage(CreateEquipmentDto request, MultipartFile image) {
        try {
            // Validate category exists
            Optional<EquipmentCategory> categoryOpt = categoryRepository.findById(request.getCategoryId());
            if (categoryOpt.isEmpty()) {
                return ApiResponse.error("Category not found");
            }

            // Upload image if provided
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                try {
                    imageUrl = cloudinaryService.uploadSingleFile(image, "equipment");
                } catch (Exception e) {
                    return ApiResponse.error("Failed to upload image: " + e.getMessage());
                }
            }

            Equipment equipment = equipmentMapper.toEntity(request);
            equipment.setCategory(categoryOpt.get());
            equipment.setImage(imageUrl);
            
            Equipment savedEquipment = equipmentRepository.save(equipment);
            return ApiResponse.success(savedEquipment, "Equipment created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to create equipment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<Equipment> updateEquipmentWithImage(Long id, CreateEquipmentDto request, MultipartFile image) {
        Optional<Equipment> existingEquipmentOpt = equipmentRepository.findById(id);
        
        if (existingEquipmentOpt.isEmpty()) {
            return ApiResponse.error("Equipment not found");
        }

        try {
            // Validate category exists
            Optional<EquipmentCategory> categoryOpt = categoryRepository.findById(request.getCategoryId());
            if (categoryOpt.isEmpty()) {
                return ApiResponse.error("Category not found");
            }

            Equipment existingEquipment = existingEquipmentOpt.get();
            String oldImageUrl = existingEquipment.getImage();
            String newImageUrl = oldImageUrl;
            
            // Handle image upload if new image is provided
            if (image != null && !image.isEmpty()) {
                try {
                    // Upload new image
                    newImageUrl = cloudinaryService.uploadSingleFile(image, "equipment");
                    
                    // Delete old image if exists and is different
                    if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                        try {
                            cloudinaryService.deleteFile(oldImageUrl);
                        } catch (Exception e) {
                            System.err.println("Failed to delete old image: " + e.getMessage());
                            // Don't fail the update if old image deletion fails
                        }
                    }
                } catch (Exception e) {
                    return ApiResponse.error("Failed to upload new image: " + e.getMessage());
                }
            }
            
            equipmentMapper.updateEntityFromDto(request, existingEquipment);
            existingEquipment.setCategory(categoryOpt.get());
            existingEquipment.setImage(newImageUrl);
            
            Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
            return ApiResponse.success(updatedEquipment, "Equipment updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to update equipment: " + e.getMessage());
        }
    }
}