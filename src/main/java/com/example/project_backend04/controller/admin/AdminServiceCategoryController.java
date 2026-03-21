package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.ServiceCategory.CreateServiceCategoryRequest;
import com.example.project_backend04.dto.request.ServiceCategory.UpdateServiceCategoryRequest;
import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.IService.IServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/service-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceCategoryController {

    private final IServiceCategoryService serviceCategoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> createServiceCategory(
            @Valid @RequestBody CreateServiceCategoryRequest request) {
        
        ApiResponse<ServiceCategoryResponse> response = serviceCategoryService.createServiceCategory(request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> updateServiceCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceCategoryRequest request) {
        
        request.setId(id);
        ApiResponse<ServiceCategoryResponse> response = serviceCategoryService.updateServiceCategory(request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteServiceCategory(@PathVariable Long id) {
        ApiResponse<String> response = serviceCategoryService.deleteServiceCategory(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> getServiceCategoryById(@PathVariable Long id) {
        ApiResponse<ServiceCategoryResponse> response = serviceCategoryService.getServiceCategoryById(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ServiceCategoryResponse>>> getAllServiceCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        ApiResponse<Page<ServiceCategoryResponse>> response = serviceCategoryService.getAllServiceCategories(page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getAllServiceCategoriesList() {
        ApiResponse<List<ServiceCategoryResponse>> response = serviceCategoryService.getAllServiceCategories();
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> searchServiceCategories(
            @RequestParam String keyword) {
        
        ApiResponse<List<ServiceCategoryResponse>> response = serviceCategoryService.searchServiceCategories(keyword);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/used-by-trainers")
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getCategoriesUsedByTrainers() {
        ApiResponse<List<ServiceCategoryResponse>> response = serviceCategoryService.getCategoriesUsedByTrainers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/used-by-services")
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getCategoriesUsedByGymServices() {
        ApiResponse<List<ServiceCategoryResponse>> response = serviceCategoryService.getCategoriesUsedByGymServices();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<String>> toggleServiceCategoryStatus(@PathVariable Long id) {
        ApiResponse<String> response = serviceCategoryService.toggleServiceCategoryStatus(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
}