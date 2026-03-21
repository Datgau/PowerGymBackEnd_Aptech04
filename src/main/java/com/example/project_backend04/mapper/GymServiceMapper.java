package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.GymServiceImage;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GymServiceMapper {

    private final ServiceRegistrationRepository serviceRegistrationRepository;

    public GymServiceResponse toResponse(GymService service) {
        if (service == null) {
            return null;
        }

        GymServiceResponse dto = new GymServiceResponse();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setPrice(service.getPrice());
        dto.setDuration(service.getDuration());
        dto.setMaxParticipants(service.getMaxParticipants());
        dto.setIsActive(service.getIsActive());
        dto.setCategory(mapCategoryToDto(service));
        dto.setImages(mapImagesToUrls(service));
        dto.setRegistrationCount(countActiveRegistrations(service));
        
        return dto;
    }

    private GymServiceResponse.ServiceCategoryDto mapCategoryToDto(GymService service) {
        try {
            if (service.getCategory() != null) {
                GymServiceResponse.ServiceCategoryDto categoryDto = new GymServiceResponse.ServiceCategoryDto();
                categoryDto.setId(service.getCategory().getId());
                categoryDto.setName(service.getCategory().getName());
                categoryDto.setDisplayName(service.getCategory().getDisplayName());
                categoryDto.setDescription(service.getCategory().getDescription());
                categoryDto.setIcon(service.getCategory().getIcon());
                categoryDto.setColor(service.getCategory().getColor());
                categoryDto.setIsActive(service.getCategory().getIsActive());
                return categoryDto;
            }
        } catch (Exception e) {
            System.err.println("Error accessing service category (lazy loading issue): " + e.getMessage());
        }
        return createDefaultCategory();
    }

    private List<String> mapImagesToUrls(GymService service) {
        try {
            if (service.getImages() != null && !service.getImages().isEmpty()) {
                return service.getImages()
                        .stream()
                        .sorted((img1, img2) -> img1.getSortOrder().compareTo(img2.getSortOrder()))
                        .map(GymServiceImage::getImageUrl)
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Error accessing service images (lazy loading issue): " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    private Long countActiveRegistrations(GymService service) {
        try {
            return serviceRegistrationRepository.countActiveRegistrations(service);
        } catch (Exception e) {
            System.err.println("Error counting registrations: " + e.getMessage());
            return 0L;
        }
    }

    private GymServiceResponse.ServiceCategoryDto createDefaultCategory() {
        GymServiceResponse.ServiceCategoryDto defaultCategory = new GymServiceResponse.ServiceCategoryDto();
        defaultCategory.setId(0L);
        defaultCategory.setName("UNKNOWN");
        defaultCategory.setDisplayName("Unknown Category");
        defaultCategory.setDescription("Category not assigned");
        defaultCategory.setIcon(null);
        defaultCategory.setColor(null);
        defaultCategory.setIsActive(true);
        defaultCategory.setSortOrder(999);
        return defaultCategory;
    }

    public List<GymServiceResponse> toResponseList(List<GymService> services) {
        if (services == null || services.isEmpty()) {
            return new ArrayList<>();
        }
        
        return services.stream()
                .map(this::toResponse)
                .toList();
    }
}