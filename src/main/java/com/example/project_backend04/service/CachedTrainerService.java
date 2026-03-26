package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Trainer.TrainerAvailabilityDTO;
import com.example.project_backend04.dto.response.TrainerBooking.TimeSlot;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.service.IService.IConflictDetectionService;
import com.example.project_backend04.service.IService.ITrainerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachedTrainerService {
    
    private final ITrainerSelectionService trainerSelectionService;
    private final IConflictDetectionService conflictDetectionService;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final TrainerBookingRepository trainerBookingRepository;
    

    @Cacheable(value = "trainer_availability", 
               key = "#serviceId + '_' + #preferredDate", 
               unless = "#result.isEmpty()")
    public List<TrainerAvailabilityDTO> getCachedAvailableTrainers(Long serviceId, LocalDate preferredDate) {
        log.debug("Cache miss - fetching available trainers for service {} on {}", serviceId, preferredDate);
        return trainerSelectionService.findAvailableTrainers(serviceId, preferredDate);
    }

    @Cacheable(value = "trainer_specialties", 
               key = "#trainerId", 
               unless = "#result.isEmpty()")
    public List<TrainerSpecialty> getCachedTrainerSpecialties(Long trainerId) {
        log.debug("Cache miss - fetching specialties for trainer {}", trainerId);
        return trainerSpecialtyRepository.findActiveSpecialtiesByTrainer(trainerId);
    }
    

    @Cacheable(value = "trainer_specialties", 
               key = "'category_' + #categoryId", 
               unless = "#result.isEmpty()")
    public List<User> getCachedTrainersByCategory(Long categoryId) {
        log.debug("Cache miss - fetching trainers for category {}", categoryId);
        return trainerSpecialtyRepository.findTrainersBySpecialtyCategory(categoryId);
    }

    @Cacheable(value = "trainer_availability", 
               key = "'slots_' + #trainerId + '_' + #date", 
               unless = "#result.isEmpty()")
    public List<TimeSlot> getCachedAvailableSlots(Long trainerId, LocalDate date) {
        log.debug("Cache miss - fetching available slots for trainer {} on {}", trainerId, date);
        return conflictDetectionService.getAvailableSlots(trainerId, date);
    }
    @Cacheable(value = "booking_statistics", 
               key = "#trainerId + '_' + #fromDate + '_' + #toDate")
    public Double getCachedTrainerAverageRating(Long trainerId) {
        log.debug("Cache miss - fetching average rating for trainer {}", trainerId);
        return trainerBookingRepository.findAverageRatingByTrainer(trainerId);
    }

    @Cacheable(value = "booking_statistics", 
               key = "'completed_' + #trainerId")
    public Long getCachedCompletedSessionsCount(Long trainerId) {
        log.debug("Cache miss - fetching completed sessions count for trainer {}", trainerId);
        return trainerBookingRepository.countByTrainerAndDateRangeAndStatus(
            trainerId, 
            LocalDate.now().minusYears(1), 
            LocalDate.now(),
            TrainerBooking.BookingStatus.COMPLETED
        );
    }

    @CacheEvict(value = "trainer_availability", 
                key = "'slots_' + #trainerId + '_' + #date")
    public void evictTrainerAvailabilityCache(Long trainerId, LocalDate date) {
        log.debug("Evicting trainer availability cache for trainer {} on {}", trainerId, date);
    }

    @CacheEvict(value = "trainer_availability", 
                allEntries = true, 
                condition = "#trainerId != null")
    public void evictAllTrainerAvailabilityCache(Long trainerId) {
        log.debug("Evicting all trainer availability cache for trainer {}", trainerId);
    }

    @CacheEvict(value = "trainer_specialties", 
                key = "#trainerId")
    public void evictTrainerSpecialtiesCache(Long trainerId) {
        log.debug("Evicting trainer specialties cache for trainer {}", trainerId);
    }

    @CacheEvict(value = "trainer_specialties", 
                key = "'category_' + #categoryId")
    public void evictCategoryTrainersCache(Long categoryId) {
        log.debug("Evicting category trainers cache for category {}", categoryId);
    }

    @CacheEvict(value = "booking_statistics", 
                allEntries = true, 
                condition = "#trainerId != null")
    public void evictBookingStatisticsCache(Long trainerId) {
        log.debug("Evicting booking statistics cache for trainer {}", trainerId);
    }

    @CacheEvict(value = {"trainer_availability", "trainer_specialties", "booking_statistics"}, 
                allEntries = true)
    public void clearAllCaches() {
        log.info("Clearing all trainer-related caches");
    }

    public void warmUpCache(List<Long> popularTrainerIds, LocalDate date) {
        log.info("Warming up cache for {} popular trainers", popularTrainerIds.size());
        
        for (Long trainerId : popularTrainerIds) {
            try {
                // Pre-load trainer specialties
                getCachedTrainerSpecialties(trainerId);
                
                // Pre-load availability for next 7 days
                for (int i = 0; i < 7; i++) {
                    LocalDate targetDate = date.plusDays(i);
                    getCachedAvailableSlots(trainerId, targetDate);
                }
                
                // Pre-load statistics
                getCachedTrainerAverageRating(trainerId);
                getCachedCompletedSessionsCount(trainerId);
                
            } catch (Exception e) {
                log.warn("Failed to warm up cache for trainer {}: {}", trainerId, e.getMessage());
            }
        }
        
        log.info("Cache warm-up completed");
    }
}