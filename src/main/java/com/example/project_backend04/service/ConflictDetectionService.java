package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.ConflictCheckResult;
import com.example.project_backend04.dto.response.TrainerBooking.TimeSlot;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.service.IService.IConflictDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictDetectionService implements IConflictDetectionService {
    
    private final TrainerBookingRepository trainerBookingRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Default working hours (can be made configurable per trainer)
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(6, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(22, 0);
    private static final int SLOT_DURATION_MINUTES = 60; // 1 hour slots
    
    @Override
    public boolean hasTimeConflict(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        log.debug("Checking time conflict for trainer {} on {} from {} to {}", 
                 trainerId, date, startTime, endTime);
        
        // Check cache first
        String cacheKey = String.format("trainer_conflicts:%d:%s", trainerId, date.toString());
        @SuppressWarnings("unchecked")
        List<TrainerBooking> cachedBookings = (List<TrainerBooking>) redisTemplate.opsForValue().get(cacheKey);
        
        List<TrainerBooking> confirmedBookings;
        if (cachedBookings != null) {
            confirmedBookings = cachedBookings;
            log.debug("Using cached bookings for trainer {} on {}", trainerId, date);
        } else {
            confirmedBookings = trainerBookingRepository.findByTrainerIdAndBookingDateAndStatus(
                trainerId, date, TrainerBooking.BookingStatus.CONFIRMED);
            
            // Cache for 1 hour
            redisTemplate.opsForValue().set(cacheKey, confirmedBookings, Duration.ofHours(1));
            log.debug("Cached bookings for trainer {} on {}", trainerId, date);
        }
        
        boolean hasConflict = confirmedBookings.stream()
            .anyMatch(booking -> booking.hasTimeConflict(date, startTime, endTime));
        
        log.debug("Conflict check result for trainer {}: {}", trainerId, hasConflict);
        return hasConflict;
    }
    
    @Override
    @Cacheable(value = "trainer_availability", key = "#trainerId + '_' + #date")
    public List<TimeSlot> getAvailableSlots(Long trainerId, LocalDate date) {
        log.debug("Getting available slots for trainer {} on {}", trainerId, date);
        
        // Get trainer's working hours (could be from trainer profile or default)
        List<TimeSlot> workingHours = getTrainerWorkingHours(trainerId, date);
        
        // Get confirmed bookings for the date
        List<TrainerBooking> confirmedBookings = trainerBookingRepository
            .findByTrainerIdAndBookingDateAndStatus(trainerId, date, TrainerBooking.BookingStatus.CONFIRMED);
        
        // Remove booked slots from working hours
        List<TimeSlot> availableSlots = new ArrayList<>();
        for (TimeSlot workingSlot : workingHours) {
            List<TimeSlot> freeSlots = subtractBookedTime(workingSlot, confirmedBookings);
            availableSlots.addAll(freeSlots);
        }
        
        // Filter slots that are at least minimum duration
        List<TimeSlot> validSlots = availableSlots.stream()
            .filter(slot -> slot.getDuration().toMinutes() >= SLOT_DURATION_MINUTES)
            .collect(Collectors.toList());
        
        log.debug("Found {} available slots for trainer {} on {}", validSlots.size(), trainerId, date);
        return validSlots;
    }
    
    @Override
    public ConflictCheckResult validateBookingRequest(CreateBookingRequest request) {
        log.debug("Validating booking request for trainer {} on {}", 
                 request.getTrainerId(), request.getBookingDate());
        
        // Basic validation
        if (!request.isValidTimeRange()) {
            return ConflictCheckResult.invalid("Start time must be before end time");
        }
        
        if (!request.isMinimumDuration()) {
            return ConflictCheckResult.invalid("Booking duration must be at least 30 minutes");
        }
        
        if (!request.isMaximumDuration()) {
            return ConflictCheckResult.invalid("Booking duration cannot exceed 8 hours");
        }
        
        // Check for time conflicts
        boolean hasConflict;
        if (request.getExcludeBookingId() != null) {
            hasConflict = hasTimeConflictExcluding(
                request.getTrainerId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getExcludeBookingId()
            );
        } else {
            hasConflict = hasTimeConflict(
                request.getTrainerId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
            );
        }
        
        if (hasConflict) {
            List<TrainerBooking> conflictingBookings = findConflictingBookings(request);
            String conflictMessage = buildConflictMessage(conflictingBookings);
            List<TimeSlot> alternatives = findAlternativeSlots(request, 5);
            
            return ConflictCheckResult.conflict(conflictingBookings, conflictMessage)
                .withAlternatives(alternatives);
        }
        
        return ConflictCheckResult.noConflict();
    }
    
    @Override
    public void reserveTimeSlot(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // Implementation for temporary reservation (could use Redis with TTL)
        String reservationKey = String.format("reservation:%d:%s:%s-%s", 
                                            trainerId, date, startTime, endTime);
        redisTemplate.opsForValue().set(reservationKey, "reserved", Duration.ofMinutes(15));
        log.debug("Reserved time slot for trainer {} on {} from {} to {}", 
                 trainerId, date, startTime, endTime);
    }
    
    @Override
    public void releaseTimeSlot(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        String reservationKey = String.format("reservation:%d:%s:%s-%s", 
                                            trainerId, date, startTime, endTime);
        redisTemplate.delete(reservationKey);
        log.debug("Released time slot for trainer {} on {} from {} to {}", 
                 trainerId, date, startTime, endTime);
    }
    
    @Override
    public List<TimeSlot> getTrainerWorkingHours(Long trainerId, LocalDate date) {
        // For now, return default working hours
        // In the future, this could be customized per trainer
        List<TimeSlot> workingHours = new ArrayList<>();
        
        // Morning session: 6:00 - 12:00
        workingHours.add(TimeSlot.of(date, LocalTime.of(6, 0), LocalTime.of(12, 0)));
        
        // Afternoon session: 14:00 - 18:00
        workingHours.add(TimeSlot.of(date, LocalTime.of(14, 0), LocalTime.of(18, 0)));
        
        // Evening session: 19:00 - 22:00
        workingHours.add(TimeSlot.of(date, LocalTime.of(19, 0), LocalTime.of(22, 0)));
        
        return workingHours;
    }
    
    @Override
    public List<TimeSlot> findAlternativeSlots(CreateBookingRequest request, int maxAlternatives) {
        Duration requestedDuration = Duration.between(request.getStartTime(), request.getEndTime());
        
        // Try same day first
        List<TimeSlot> sameDay = getAvailableSlots(request.getTrainerId(), request.getBookingDate())
            .stream()
            .filter(slot -> slot.getDuration().compareTo(requestedDuration) >= 0)
            .limit(3)
            .collect(Collectors.toList());
        
        if (!sameDay.isEmpty()) {
            return sameDay;
        }
        
        // Try next 7 days
        List<TimeSlot> alternatives = new ArrayList<>();
        for (int i = 1; i <= 7 && alternatives.size() < maxAlternatives; i++) {
            LocalDate alternativeDate = request.getBookingDate().plusDays(i);
            List<TimeSlot> daySlots = getAvailableSlots(request.getTrainerId(), alternativeDate)
                .stream()
                .filter(slot -> slot.getDuration().compareTo(requestedDuration) >= 0)
                .limit(2)
                .collect(Collectors.toList());
            alternatives.addAll(daySlots);
        }
        
        return alternatives.stream().limit(maxAlternatives).collect(Collectors.toList());
    }
    
    @Override
    public boolean hasTimeConflictExcluding(Long trainerId, LocalDate date, LocalTime startTime, 
                                          LocalTime endTime, Long excludeBookingId) {
        List<TrainerBooking> conflictingBookings = trainerBookingRepository
            .findConflictingBookingsExcluding(trainerId, date, startTime, endTime, excludeBookingId);
        
        return !conflictingBookings.isEmpty();
    }
    
    private List<TrainerBooking> findConflictingBookings(CreateBookingRequest request) {
        if (request.getExcludeBookingId() != null) {
            return trainerBookingRepository.findConflictingBookingsExcluding(
                request.getTrainerId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getExcludeBookingId()
            );
        } else {
            // Create a query that uses trainerId directly
            return trainerBookingRepository.findConflictingBookingsByTrainerId(
                request.getTrainerId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
            );
        }
    }
    
    private String buildConflictMessage(List<TrainerBooking> conflictingBookings) {
        if (conflictingBookings.isEmpty()) {
            return "No conflicts found";
        }
        
        if (conflictingBookings.size() == 1) {
            TrainerBooking booking = conflictingBookings.get(0);
            return String.format("Time slot conflicts with existing booking from %s to %s", 
                               booking.getStartTime(), booking.getEndTime());
        }
        
        return String.format("Time slot conflicts with %d existing bookings", 
                           conflictingBookings.size());
    }
    
    private List<TimeSlot> subtractBookedTime(TimeSlot workingSlot, List<TrainerBooking> bookings) {
        List<TimeSlot> result = new ArrayList<>();
        LocalTime currentStart = workingSlot.getStartTime();
        LocalTime slotEnd = workingSlot.getEndTime();
        
        // Sort bookings by start time
        List<TrainerBooking> sortedBookings = bookings.stream()
            .filter(booking -> booking.getBookingDate().equals(workingSlot.getDate()))
            .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
            .collect(Collectors.toList());
        
        for (TrainerBooking booking : sortedBookings) {
            // If there's a gap before this booking, add it as available
            if (currentStart.isBefore(booking.getStartTime())) {
                result.add(TimeSlot.of(workingSlot.getDate(), currentStart, booking.getStartTime()));
            }
            
            // Move current start to after this booking
            if (booking.getEndTime().isAfter(currentStart)) {
                currentStart = booking.getEndTime();
            }
        }
        
        // Add remaining time after last booking
        if (currentStart.isBefore(slotEnd)) {
            result.add(TimeSlot.of(workingSlot.getDate(), currentStart, slotEnd));
        }
        
        return result;
    }
}