package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.GymServiceRequest;
import com.example.project_backend04.dto.request.Service.UpdateGymServiceDto;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.GymServiceImage;
import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.mapper.GymServiceMapper;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.ServiceCategoryRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.service.IService.ICloudinaryService;
import com.example.project_backend04.service.IService.IGymService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GymServiceService implements IGymService {

    private final GymServiceRepository gymServiceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ICloudinaryService cloudStorageService;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final GymServiceMapper gymServiceMapper;

    private static final String SERVICE_FOLDER = "services";

    @Transactional(readOnly = true)
    public List<GymServiceResponse> getPublicServices() {
        List<GymService> services = gymServiceRepository.findByIsActiveTrueWithImages();
        return gymServiceMapper.toResponseList(services);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GymServiceResponse> getPublicServices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<GymService> servicePage = gymServiceRepository.findByIsActiveTrueWithImagesPaginated(pageable);
        List<Long> ids = servicePage.getContent().stream()
                .map(GymService::getId)
                .toList();
        
        if (!ids.isEmpty()) {
            List<GymService> servicesWithImages = gymServiceRepository.findByIdsWithImages(ids);
            // Map back to maintain order
            return servicePage.map(service -> {
                GymService withImages = servicesWithImages.stream()
                        .filter(s -> s.getId().equals(service.getId()))
                        .findFirst()
                        .orElse(service);
                return gymServiceMapper.toResponse(withImages);
            });
        }
        
        return servicePage.map(gymServiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public GymServiceResponse getServiceById(Long id) {
        GymService service = gymServiceRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        return gymServiceMapper.toResponse(service);
    }
    @Transactional(readOnly = true)
    public Map<String, Object> getServiceRegistrationStats(Long id) {
        GymService service = gymServiceRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        
        Long registrationCount = serviceRegistrationRepository.countActiveRegistrations(service);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("serviceId", id);
        stats.put("serviceName", service.getName());
        stats.put("registrationCount", registrationCount);
        stats.put("maxParticipants", service.getMaxParticipants());
        stats.put("availableSlots", service.getMaxParticipants() - registrationCount);
        stats.put("isFullyBooked", registrationCount >= service.getMaxParticipants());
        
        return stats;
    }
    @Transactional(readOnly = true)
    public List<GymServiceResponse> getAllServices() {
        List<GymService> services = gymServiceRepository.findAllWithImages();
        return gymServiceMapper.toResponseList(services);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<GymServiceResponse> getAllServices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<GymService> servicePage = gymServiceRepository.findAllWithImagesPaginated(pageable);
        List<Long> ids = servicePage.getContent().stream()
                .map(GymService::getId)
                .toList();
        
        if (!ids.isEmpty()) {
            List<GymService> servicesWithImages = gymServiceRepository.findByIdsWithImages(ids);
            return servicePage.map(service -> {
                GymService withImages = servicesWithImages.stream()
                        .filter(s -> s.getId().equals(service.getId()))
                        .findFirst()
                        .orElse(service);
                return gymServiceMapper.toResponse(withImages);
            });
        }
        
        return servicePage.map(gymServiceMapper::toResponse);
    }
    @Transactional
    public GymServiceResponse createService(GymServiceRequest request) {
        validateServiceRequest(request);
        ServiceCategory category = serviceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("ServiceCategory not found with id: " + request.getCategoryId()));

        GymService service = new GymService();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setCategory(category);
        service.setPrice(request.getPrice());
        service.setDuration(request.getDuration());
        service.setMaxParticipants(request.getMaxParticipants());
        service.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        if (request.getTrainerPercentage() != null) {
            service.setTrainerPercentage(request.getTrainerPercentage());
        }

        GymService savedService = gymServiceRepository.save(service);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<GymServiceImage> serviceImages = new ArrayList<>();
            CloudinaryService cloudinaryServiceImpl = (CloudinaryService) cloudStorageService;
            
            for (int i = 0; i < request.getImages().size(); i++) {
                try {
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
        
        GymService reloadedService = gymServiceRepository.findByIdWithImages(savedService.getId())
                .orElseThrow(() -> new RuntimeException("Failed to reload created service"));
        
        return gymServiceMapper.toResponse(reloadedService);
    }

    @Transactional
    public GymServiceResponse updateService(Long id, UpdateGymServiceDto request) {

        GymService service = gymServiceRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (request.getName() != null)
            service.setName(request.getName());

        if (request.getDescription() != null)
            service.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            ServiceCategory category = serviceCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("ServiceCategory not found with id: " + request.getCategoryId()));
            service.setCategory(category);
        }

        if (request.getPrice() != null)
            service.setPrice(request.getPrice());

        if (request.getDuration() != null)
            service.setDuration(request.getDuration());

        if (request.getMaxParticipants() != null)
            service.setMaxParticipants(request.getMaxParticipants());

        if (request.getIsActive() != null)
            service.setIsActive(request.getIsActive());
        if (request.getTrainerPercentage() != null)
            service.setTrainerPercentage(request.getTrainerPercentage());
        if (request.getDeletedImages() != null && !request.getDeletedImages().isEmpty()) {
            List<GymServiceImage> imagesToDelete = service.getImages().stream()
                    .filter(img -> request.getDeletedImages().contains(img.getImageUrl()))
                    .toList();
            
            for (GymServiceImage imageToDelete : imagesToDelete) {
                try {
                    cloudStorageService.deleteFile(imageToDelete.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Failed to delete image from cloud storage: " + e.getMessage());
                }
                service.getImages().remove(imageToDelete);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            int currentMaxOrder = service.getImages().stream()
                    .mapToInt(GymServiceImage::getSortOrder)
                    .max()
                    .orElse(0);

            for (int i = 0; i < request.getImages().size(); i++) {
                try {
                    String imageUrl = cloudStorageService.uploadSingleFile(request.getImages().get(i), SERVICE_FOLDER);
                    
                    GymServiceImage serviceImage = new GymServiceImage();
                    serviceImage.setGymService(service);
                    serviceImage.setImageUrl(imageUrl);
                    serviceImage.setSortOrder(currentMaxOrder + i + 1);
                    
                    service.getImages().add(serviceImage);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to upload image: " + e.getMessage());
                }
            }
        }

        return gymServiceMapper.toResponse(gymServiceRepository.save(service));
    }

    @Transactional
    public void deleteService(Long id) {
        GymService service = gymServiceRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));

        Long activeRegistrations = serviceRegistrationRepository.countActiveRegistrations(service);
        if (activeRegistrations > 0) {
            throw new RuntimeException("Cannot delete this service because there are "
                    + activeRegistrations + " active registrations");
        }

        if (service.getImages() != null && !service.getImages().isEmpty()) {
            service.getImages().forEach(serviceImage -> {
                try {
                    cloudStorageService.deleteFile(serviceImage.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Failed to delete image: " + serviceImage.getImageUrl());
                    e.printStackTrace();
                }
            });
        }

        gymServiceRepository.delete(service);
    }

    private void validateServiceRequest(GymServiceRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name is required");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Service category ID is required");
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
}
