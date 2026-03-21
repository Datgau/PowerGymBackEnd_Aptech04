package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/service-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServiceCategorySeederController {

    private final ServiceCategoryRepository serviceCategoryRepository;
    @PostMapping("/seed-defaults")
    public ResponseEntity<ApiResponse<String>> seedDefaultCategories() {
        try {
            // Kiểm tra xem đã có data chưa
            if (serviceCategoryRepository.count() > 0) {
                return ResponseEntity.ok(new ApiResponse<>(
                    false, 
                    "Service categories already exist. Use individual create endpoints to add more.", 
                    null, 
                    400
                ));
            }

            List<ServiceCategory> defaultCategories = Arrays.asList(
                createCategory("PERSONAL_TRAINER", "Personal Trainer", 
                    "One-on-one fitness training with certified personal trainers", 
                    "person", "#FF5722", 0),
                    
                createCategory("BOXING", "Boxing", 
                    "Boxing training for fitness and self-defense", 
                    "sports_mma", "#F44336", 1),
                    
                createCategory("YOGA", "Yoga", 
                    "Yoga classes for flexibility, strength and mindfulness", 
                    "self_improvement", "#4CAF50", 2),
                    
                createCategory("CARDIO", "Cardio", 
                    "Cardiovascular exercises and training programs", 
                    "favorite", "#2196F3", 3),
                    
                createCategory("GYM", "Gym", 
                    "General gym access and equipment usage", 
                    "fitness_center", "#9C27B0", 4),
                    
                createCategory("OTHER", "Other", 
                    "Other fitness and wellness services", 
                    "more_horiz", "#607D8B", 5)
            );

            serviceCategoryRepository.saveAll(defaultCategories);

            return ResponseEntity.ok(new ApiResponse<>(
                true, 
                String.format("Successfully seeded %d default service categories", defaultCategories.size()), 
                null, 
                200
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(
                false, 
                "Error seeding default categories: " + e.getMessage(), 
                null, 
                500
            ));
        }
    }
    /**
     * Reset tất cả categories về default (NGUY HIỂM - chỉ dùng trong development)
     */
    @PostMapping("/reset-to-defaults")
    @PreAuthorize("hasRole('ADMIN')") // Có thể thêm thêm security check
    public ResponseEntity<ApiResponse<String>> resetToDefaults(
            @RequestParam(defaultValue = "false") boolean confirmReset) {
        
        if (!confirmReset) {
            return ResponseEntity.ok(new ApiResponse<>(
                false, 
                "This action will DELETE ALL existing categories. Add ?confirmReset=true to confirm.", 
                null, 
                400
            ));
        }

        try {
            serviceCategoryRepository.deleteAll();
            
            // Seed lại default data
            return seedDefaultCategories();

        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(
                false, 
                "Error resetting categories: " + e.getMessage(), 
                null, 
                500
            ));
        }
    }

    private ServiceCategory createCategory(String name, String displayName, String description, 
                                         String icon, String color, int sortOrder) {
        ServiceCategory category = new ServiceCategory();
        category.setName(name);
        category.setDisplayName(displayName);
        category.setDescription(description);
        category.setIcon(icon);
        category.setColor(color);
        category.setIsActive(true);
        return category;
    }
}