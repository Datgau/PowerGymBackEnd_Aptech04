package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Equipment.CreateEquipmentDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.Equipment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEquipmentService {
    ApiResponse<List<Equipment>> getAllEquipments();
    ApiResponse<List<Equipment>> getActiveEquipments();
    ApiResponse<List<Equipment>> getEquipmentsByCategory(Long categoryId);
    ApiResponse<List<Equipment>> getActiveEquipmentsByCategory(Long categoryId);
    ApiResponse<Equipment> getEquipmentById(Long id);
    ApiResponse<Equipment> createEquipment(CreateEquipmentDto request);
    ApiResponse<Equipment> updateEquipment(Long id, CreateEquipmentDto request);
    ApiResponse<Void> deleteEquipment(Long id);
    
    // New methods with image handling
    ApiResponse<Equipment> createEquipmentWithImage(CreateEquipmentDto request, MultipartFile image);
    ApiResponse<Equipment> updateEquipmentWithImage(Long id, CreateEquipmentDto request, MultipartFile image);
}