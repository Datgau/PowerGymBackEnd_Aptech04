package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service lấy danh sách trainer phù hợp với service để hiển thị trong TrainerStep.
 * Logic đơn giản: lấy trainers có specialty khớp với category của service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerForBookingService {

    private final GymServiceRepository gymServiceRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;

    /**
     * Lấy danh sách trainer phù hợp với service (theo category).
     * Dùng cho endpoint GET /public/trainers/specialty-category/{categoryId}
     */
    @Transactional(readOnly = true)
    public List<TrainerForBookingResponse> getTrainersByServiceId(Long serviceId) {
        GymService service = gymServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + serviceId));

        Long categoryId = service.getCategory().getId();
        log.debug("Loading trainers for service {} (category {})", serviceId, categoryId);

        List<TrainerSpecialty> specialties = trainerSpecialtyRepository
                .findTrainerSpecialtiesByCategory(categoryId);

        // Group by trainer, map to response
        return specialties.stream()
                .collect(Collectors.groupingBy(ts -> ts.getUser().getId()))
                .values().stream()
                .filter(trainerSpecialties -> {
                    var user = trainerSpecialties.get(0).getUser();
                    return Boolean.TRUE.equals(user.getIsActive());
                })
                .map(trainerSpecialties -> {
                    var user = trainerSpecialties.get(0).getUser();
                    return TrainerForBookingResponse.builder()
                            .id(user.getId())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .avatar(user.getAvatar())
                            .bio(user.getBio())
                            .totalExperienceYears(user.getTotalExperienceYears())
                            .isActive(user.getIsActive())
                            .specialties(trainerSpecialties.stream()
                                    .map(ts -> TrainerForBookingResponse.SpecialtyInfo.builder()
                                            .id(ts.getId())
                                            .specialty(TrainerForBookingResponse.CategoryInfo.builder()
                                                    .id(ts.getSpecialty().getId())
                                                    .name(ts.getSpecialty().getName())
                                                    .displayName(ts.getSpecialty().getDisplayName())
                                                    .build())
                                            .experienceYears(ts.getExperienceYears())
                                            .level(ts.getLevel())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
