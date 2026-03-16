package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.ServiceCategory.CreateServiceCategoryRequest;
import com.example.project_backend04.dto.request.ServiceCategory.UpdateServiceCategoryRequest;
import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.repository.ServiceCategoryRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.service.IService.IServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceCategoryService implements IServiceCategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final GymServiceRepository gymServiceRepository;

    @Override
    public ApiResponse<ServiceCategoryResponse> createServiceCategory(CreateServiceCategoryRequest request) {
        try {
            // Kiểm tra name đã tồn tại
            if (serviceCategoryRepository.existsByNameIgnoreCase(request.getName())) {
                return new ApiResponse<>(false, "Category name đã tồn tại", null, 400);
            }

            // Tạo entity
            ServiceCategory category = new ServiceCategory();
            category.setName(request.getName().toUpperCase());
            category.setDisplayName(request.getDisplayName());
            category.setDescription(request.getDescription());
            category.setIcon(request.getIcon());
            category.setColor(request.getColor());
            category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : getNextSortOrder());
            category.setIsActive(true);

            ServiceCategory savedCategory = serviceCategoryRepository.save(category);
            ServiceCategoryResponse response = mapToResponse(savedCategory);

            return new ApiResponse<>(true, "Tạo service category thành công", response, 201);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi tạo service category: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<ServiceCategoryResponse> updateServiceCategory(UpdateServiceCategoryRequest request) {
        try {
            ServiceCategory category = serviceCategoryRepository.findById(request.getId())
                    .orElse(null);

            if (category == null) {
                return new ApiResponse<>(false, "Không tìm thấy service category", null, 404);
            }

            // Cập nhật thông tin
            category.setDisplayName(request.getDisplayName());
            category.setDescription(request.getDescription());
            category.setIcon(request.getIcon());
            category.setColor(request.getColor());
            category.setSortOrder(request.getSortOrder());
            category.setIsActive(request.getIsActive());

            ServiceCategory updatedCategory = serviceCategoryRepository.save(category);
            ServiceCategoryResponse response = mapToResponse(updatedCategory);

            return new ApiResponse<>(true, "Cập nhật service category thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi cập nhật service category: " + e.getMessage(), null, 500);
        }
    }
    @Override
    public ApiResponse<String> deleteServiceCategory(Long id) {
        try {
            ServiceCategory category = serviceCategoryRepository.findById(id).orElse(null);
            if (category == null) {
                return new ApiResponse<>(false, "Không tìm thấy service category", null, 404);
            }

            long trainerCount = trainerSpecialtyRepository.countBySpecialtyAndIsActiveTrue(category);
            long serviceCount = gymServiceRepository.countByCategoryAndIsActiveTrue(category);

            if (trainerCount > 0 || serviceCount > 0) {
                return new ApiResponse<>(false, 
                    String.format("Không thể xóa category đang được sử dụng bởi %d trainers và %d services", 
                    trainerCount, serviceCount), null, 400);
            }

            serviceCategoryRepository.delete(category);
            return new ApiResponse<>(true, "Xóa service category thành công", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi xóa service category: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<ServiceCategoryResponse> getServiceCategoryById(Long id) {
        try {
            ServiceCategory category = serviceCategoryRepository.findById(id).orElse(null);
            if (category == null) {
                return new ApiResponse<>(false, "Không tìm thấy service category", null, 404);
            }

            ServiceCategoryResponse response = mapToResponse(category);
            return new ApiResponse<>(true, "Lấy service category thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy service category: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<ServiceCategoryResponse> getServiceCategoryByName(String name) {
        try {
            ServiceCategory category = serviceCategoryRepository.findByNameIgnoreCase(name).orElse(null);
            if (category == null) {
                return new ApiResponse<>(false, "Không tìm thấy service category", null, 404);
            }

            ServiceCategoryResponse response = mapToResponse(category);
            return new ApiResponse<>(true, "Lấy service category thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy service category: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<List<ServiceCategoryResponse>> getAllServiceCategories() {
        try {
            List<ServiceCategory> categories = serviceCategoryRepository.findAll(Sort.by("sortOrder", "displayName"));
            List<ServiceCategoryResponse> response = categories.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Lấy danh sách service categories thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách service categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<List<ServiceCategoryResponse>> getAllActiveServiceCategories() {
        try {
            List<ServiceCategory> categories = serviceCategoryRepository.findAllActiveOrderBySortOrder();
            List<ServiceCategoryResponse> response = categories.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Lấy danh sách active service categories thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách active service categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<Page<ServiceCategoryResponse>> getAllServiceCategories(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder", "displayName"));
            Page<ServiceCategory> categories = serviceCategoryRepository.findAll(pageable);
            
            Page<ServiceCategoryResponse> response = categories.map(this::mapToResponse);
            return new ApiResponse<>(true, "Lấy danh sách service categories thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách service categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<List<ServiceCategoryResponse>> searchServiceCategories(String keyword) {
        try {
            List<ServiceCategory> categories = serviceCategoryRepository
                    .findByDisplayNameContainingIgnoreCaseAndIsActiveTrue(keyword);
            
            List<ServiceCategoryResponse> response = categories.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Tìm kiếm service categories thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi tìm kiếm service categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<List<ServiceCategoryResponse>> getCategoriesUsedByTrainers() {
        try {
            List<ServiceCategory> categories = serviceCategoryRepository.findCategoriesUsedByTrainers();
            List<ServiceCategoryResponse> response = categories.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Lấy categories được sử dụng bởi trainers thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<List<ServiceCategoryResponse>> getCategoriesUsedByGymServices() {
        try {
            List<ServiceCategory> categories = serviceCategoryRepository.findCategoriesUsedByGymServices();
            List<ServiceCategoryResponse> response = categories.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Lấy categories được sử dụng bởi gym services thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> reorderServiceCategories(List<Long> categoryIds) {
        try {
            for (int i = 0; i < categoryIds.size(); i++) {
                Long categoryId = categoryIds.get(i);
                ServiceCategory category = serviceCategoryRepository.findById(categoryId).orElse(null);
                if (category != null) {
                    category.setSortOrder(i);
                    serviceCategoryRepository.save(category);
                }
            }

            return new ApiResponse<>(true, "Sắp xếp lại service categories thành công", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi sắp xếp lại service categories: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> toggleServiceCategoryStatus(Long id) {
        try {
            ServiceCategory category = serviceCategoryRepository.findById(id).orElse(null);
            if (category == null) {
                return new ApiResponse<>(false, "Không tìm thấy service category", null, 404);
            }

            category.setIsActive(!category.getIsActive());
            serviceCategoryRepository.save(category);

            String status = category.getIsActive() ? "kích hoạt" : "vô hiệu hóa";
            return new ApiResponse<>(true, "Đã " + status + " service category thành công", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi thay đổi trạng thái service category: " + e.getMessage(), null, 500);
        }
    }

    // Helper methods
    private ServiceCategoryResponse mapToResponse(ServiceCategory category) {
        ServiceCategoryResponse response = new ServiceCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDisplayName(category.getDisplayName());
        response.setDescription(category.getDescription());
        response.setIcon(category.getIcon());
        response.setColor(category.getColor());
        response.setIsActive(category.getIsActive());
        response.setSortOrder(category.getSortOrder());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());

        // Add statistics
        response.setTrainerCount(trainerSpecialtyRepository.countBySpecialtyAndIsActiveTrue(category));
        response.setServiceCount(gymServiceRepository.countByCategoryAndIsActiveTrue(category));

        return response;
    }

    private Integer getNextSortOrder() {
        Integer maxOrder = serviceCategoryRepository.findMaxSortOrder();
        return maxOrder != null ? maxOrder + 1 : 0;
    }
}