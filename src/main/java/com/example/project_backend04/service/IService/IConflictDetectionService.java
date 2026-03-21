package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.ConflictCheckResult;
import com.example.project_backend04.dto.response.TrainerBooking.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IConflictDetectionService {
    
    /**
     * Check if trainer has time conflict for specific time slot
     */
    boolean hasTimeConflict(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime);
    
    /**
     * Get available time slots for trainer on specific date
     */
    List<TimeSlot> getAvailableSlots(Long trainerId, LocalDate date);
    
    /**
     * Validate booking request for conflicts and business rules
     */
    ConflictCheckResult validateBookingRequest(CreateBookingRequest request);
    
    /**
     * Reserve time slot temporarily (for booking process)
     */
    void reserveTimeSlot(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime);
    
    /**
     * Release reserved time slot
     */
    void releaseTimeSlot(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime);
    
    /**
     * Get trainer's working hours for specific date
     */
    List<TimeSlot> getTrainerWorkingHours(Long trainerId, LocalDate date);
    
    /**
     * Find alternative time slots when conflict occurs
     */
    List<TimeSlot> findAlternativeSlots(CreateBookingRequest request, int maxAlternatives);
    
    /**
     * Check conflicts excluding specific booking (for rescheduling)
     */
    boolean hasTimeConflictExcluding(Long trainerId, LocalDate date, LocalTime startTime, 
                                   LocalTime endTime, Long excludeBookingId);
}