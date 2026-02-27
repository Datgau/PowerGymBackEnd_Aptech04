package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.GymServiceRequest;
import com.example.project_backend04.dto.request.Service.UpdateGymServiceDto;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.GymServiceImage;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.service.IService.ICloudinaryService;
import com.example.project_backend04.service.IService.IGymService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymServiceService implements IGymService {

    private final GymServiceRepository gymServiceRepository;
    private final ICloudinaryService cloudStorageService;

    private static final String SERVICE_FOLDER = "services";

    // ===================== GET PUBLIC SERVICES =====================
    public List<GymServiceResponse> getPublicServices() {
        return gymServiceRepository.findByIsActiveTrueWithImages()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // ===================== GET SERVICE BY ID =====================
    public GymServiceResponse getServiceById(Long id) {
        GymService service = gymServiceRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        return mapToDto(service);
    }

    // ===================== GET ALL SERVICES =====================
    public List<GymServiceResponse> getAllServices() {
        return gymServiceRepository.findAllWithImages()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // ===================== CREATE =====================
    public GymServiceResponse createService(GymServiceRequest request) {
        validateServiceRequest(request);

        GymService service = new GymService();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setCategory(request.getCategory());
        service.setPrice(request.getPrice());
        service.setDuration(request.getDuration());
        service.setMaxParticipants(request.getMaxParticipants());
        service.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        GymService savedService = gymServiceRepository.save(service);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<GymServiceImage> serviceImages = new ArrayList<>();
            CloudinaryService cloudinaryServiceImpl = (CloudinaryService) cloudStorageService;
            
            for (int i = 0; i < request.getImages().size(); i++) {
                try {
                    // Upload với optimization (1600x900 cho service images)
                    String imageUrl = cloudinaryServiceImpl.uploadServiceImage(request.getImages().get(i));
                    
                    GymServiceImage serviceImage = new GymServiceImage();
                    serviceImage.setImageUrl(imageUrl);
                    serviceImage.setSortOrder(i + 1);
                    serviceImage.setGymService(savedService);
                    
                    serviceImages.add(serviceImage);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Upload image failed", e);
                }
            }

            savedService.setImages(serviceImages);
            savedService = gymServiceRepository.save(savedService);
        }

        return mapToDto(savedService);
    }

    // ===================== UPDATE =====================
    public GymServiceResponse updateService(Long id, UpdateGymServiceDto request) {

        GymService service = gymServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (request.getName() != null)
            service.setName(request.getName());

        if (request.getDescription() != null)
            service.setDescription(request.getDescription());

        if (request.getCategory() != null)
            service.setCategory(request.getCategory());

        if (request.getPrice() != null)
            service.setPrice(request.getPrice());

        if (request.getDuration() != null)
            service.setDuration(request.getDuration());

        if (request.getMaxParticipants() != null)
            service.setMaxParticipants(request.getMaxParticipants());

        if (request.getIsActive() != null)
            service.setIsActive(request.getIsActive());

        return mapToDto(gymServiceRepository.save(service));
    }

    public void deleteService(Long id) {
        GymService service = gymServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));

        if (service.getImages() != null && !service.getImages().isEmpty()) {
            service.getImages().forEach(serviceImage -> {
                try {
                    cloudStorageService.deleteFile(serviceImage.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Failed to delete image: " + serviceImage.getImageUrl());
                }
            });
        }

        gymServiceRepository.delete(service);
    }

    private void validateServiceRequest(GymServiceRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name is required");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Service category is required");
        }
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (request.getDuration() != null && request.getDuration() <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        if (request.getMaxParticipants() != null && request.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("Max participants must be positive");
        }
    }

    private GymServiceResponse mapToDto(GymService service) {
        GymServiceResponse dto = new GymServiceResponse();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setCategory(service.getCategory());
        if (service.getImages() != null && !service.getImages().isEmpty()) {
            List<String> imageUrls = service.getImages()
                    .stream()
                    .sorted((img1, img2) -> img1.getSortOrder().compareTo(img2.getSortOrder()))
                    .map(GymServiceImage::getImageUrl)
                    .toList();
            dto.setImages(imageUrls);
        } else {
            dto.setImages(new ArrayList<>());
        }
        
        dto.setPrice(service.getPrice());
        dto.setDuration(service.getDuration());
        dto.setMaxParticipants(service.getMaxParticipants());
        dto.setIsActive(service.getIsActive());
        return dto;
    }
}
