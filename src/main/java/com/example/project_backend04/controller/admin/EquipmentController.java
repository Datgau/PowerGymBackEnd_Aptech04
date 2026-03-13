package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.Equipment.CreateEquipmentDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.Equipment;
import com.example.project_backend04.service.IService.IEquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/equipments")
@RequiredArgsConstructor
public class EquipmentController {
    
    private final IEquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Equipment>>> getAllEquipments() {
        try {
            ApiResponse<List<Equipment>> response = equipmentService.getAllEquipments();
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<Equipment>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Equipment>>> getActiveEquipments() {
        try {
            ApiResponse<List<Equipment>> response = equipmentService.getActiveEquipments();
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<Equipment>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Equipment>>> getEquipmentsByCategory(@PathVariable Long categoryId) {
        try {
            ApiResponse<List<Equipment>> response = equipmentService.getEquipmentsByCategory(categoryId);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<Equipment>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/category/{categoryId}/active")
    public ResponseEntity<ApiResponse<List<Equipment>>> getActiveEquipmentsByCategory(@PathVariable Long categoryId) {
        try {
            ApiResponse<List<Equipment>> response = equipmentService.getActiveEquipmentsByCategory(categoryId);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<Equipment>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> getEquipmentById(@PathVariable Long id) {
        ApiResponse<Equipment> response = equipmentService.getEquipmentById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Equipment>> createEquipment(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") String price,
            @RequestParam("quantity") String quantity,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "isActive", defaultValue = "true") String isActive,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            CreateEquipmentDto request = new CreateEquipmentDto();
            request.setName(name);
            request.setDescription(description);
            request.setPrice(new java.math.BigDecimal(price));
            request.setQuantity(Integer.parseInt(quantity));
            request.setCategoryId(Long.parseLong(categoryId));
            request.setIsActive(Boolean.parseBoolean(isActive));
            
            ApiResponse<Equipment> response = equipmentService.createEquipmentWithImage(request, image);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<Equipment> errorResponse = 
                ApiResponse.error("Invalid request data: " + e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> updateEquipment(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") String price,
            @RequestParam("quantity") String quantity,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "isActive", defaultValue = "true") String isActive,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            CreateEquipmentDto request = new CreateEquipmentDto();
            request.setName(name);
            request.setDescription(description);
            request.setPrice(new java.math.BigDecimal(price));
            request.setQuantity(Integer.parseInt(quantity));
            request.setCategoryId(Long.parseLong(categoryId));
            request.setIsActive(Boolean.parseBoolean(isActive));
            
            ApiResponse<Equipment> response = equipmentService.updateEquipmentWithImage(id, request, image);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<Equipment> errorResponse = 
                ApiResponse.error("Invalid request data: " + e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEquipment(@PathVariable Long id) {
        ApiResponse<Void> response = equipmentService.deleteEquipment(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}