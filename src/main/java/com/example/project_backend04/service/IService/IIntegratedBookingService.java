package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.TrainerBooking.CreateServiceLinkedBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.BookingStatistics;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IIntegratedBookingService {
    
    /**
     * Create booking linked to service registration
     */
    TrainerBookingResponse createServiceLinkedBooking(CreateServiceLinkedBookingRequest request);
    
    /**
     * Get all bookings for a service registration
     */
    List<TrainerBookingResponse> getServiceBookings(Long serviceRegistrationId);
    
    /**
     * Trainer confirms or rejects booking
     */
    TrainerBookingResponse confirmBooking(Long bookingId, String trainerNotes);
    
    /**
     * Trainer rejects booking
     */
    TrainerBookingResponse rejectBooking(Long bookingId, String rejectionReason);
    
    /**
     * Reschedule booking to new time
     */
    TrainerBookingResponse rescheduleBooking(Long bookingId, LocalDate newDate, LocalTime newStartTime);
    
    /**
     * Complete session with feedback and rating
     */
    TrainerBookingResponse completeSession(Long bookingId, String clientFeedback, 
                                         Integer rating, String trainerNotes);
    
    /**
     * Cancel booking
     */
    TrainerBookingResponse cancelBooking(Long bookingId, String cancellationReason);
    
    /**
     * Mark booking as no-show
     */
    TrainerBookingResponse markNoShow(Long bookingId, String notes);
    
    /**
     * Get booking statistics for trainer
     */
    BookingStatistics getBookingStatistics(Long trainerId, LocalDate fromDate, LocalDate toDate);
    
    /**
     * Get pending bookings for trainer
     */
    List<TrainerBookingResponse> getPendingBookings(Long trainerId);
    
    /**
     * Get upcoming bookings for user
     */
    List<TrainerBookingResponse> getUpcomingBookings(Long userId);
    
    /**
     * Get booking history for user
     */
    List<TrainerBookingResponse> getBookingHistory(Long userId, int page, int size);
}