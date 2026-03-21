package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
import com.example.project_backend04.dto.request.TrainerBooking.CreateServiceLinkedBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.BookingStatistics;
import com.example.project_backend04.dto.response.TrainerBooking.ConflictCheckResult;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.exception.BookingConflictException;
import com.example.project_backend04.mapper.TrainerMapper;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.service.IService.IConflictDetectionService;
import com.example.project_backend04.service.IService.IIntegratedBookingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IntegratedBookingService implements IIntegratedBookingService {
    
    private final TrainerBookingRepository trainerBookingRepository;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final IConflictDetectionService conflictDetectionService;
    private final TrainerMapper trainerMapper;
    private final NotificationService notificationService;
    
    @Override
    public TrainerBookingResponse createServiceLinkedBooking(CreateServiceLinkedBookingRequest request) {
        log.info("Creating service-linked booking for registration {}", request.getServiceRegistrationId());
        
        // Validate service registration
        ServiceRegistration registration = serviceRegistrationRepository.findById(request.getServiceRegistrationId())
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        if (!registration.canBookTrainer()) {
            throw new IllegalStateException("Cannot book trainer for this service registration");
        }
        
        // Validate request
        if (!request.isValidTimeRange()) {
            throw new IllegalArgumentException("Invalid time range: start time must be before end time");
        }
        
        if (!request.isValidDuration()) {
            throw new IllegalArgumentException("Invalid duration: must be between 30 minutes and 8 hours");
        }
        
        // Check for conflicts
        CreateBookingRequest conflictRequest = CreateBookingRequest.builder()
            .trainerId(registration.getTrainer().getId())
            .bookingDate(request.getBookingDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .build();
        
        ConflictCheckResult conflictCheck = conflictDetectionService.validateBookingRequest(conflictRequest);
        
        if (conflictCheck.isHasConflict()) {
            throw new BookingConflictException("Time slot conflicts with existing booking", conflictCheck);
        }
        
        // Create booking
        TrainerBooking booking = TrainerBooking.builder()
            .bookingId(generateBookingId())
            .user(registration.getUser())
            .trainer(registration.getTrainer())
            .serviceRegistration(registration)
            .bookingDate(request.getBookingDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .sessionObjective(request.getSessionObjective())
            .sessionNumber(request.getSessionNumber())
            .notes(request.getNotes())
            .status(TrainerBooking.BookingStatus.PENDING)
            .build();
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send notifications
        notificationService.notifyTrainerNewBookingRequest(saved);
        notificationService.notifyClientBookingCreated(saved);
        
        log.info("Successfully created service-linked booking {}", saved.getBookingId());
        return mapToBookingResponse(saved);
    }
    
    @Override
    public List<TrainerBookingResponse> getServiceBookings(Long serviceRegistrationId) {
        log.debug("Getting bookings for service registration {}", serviceRegistrationId);
        
        List<TrainerBooking> bookings = trainerBookingRepository
            .findByServiceRegistrationWithDetails(serviceRegistrationId);
        
        return bookings.stream()
            .map(this::mapToBookingResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public TrainerBookingResponse confirmBooking(Long bookingId, String trainerNotes) {
        log.info("Confirming booking {}", bookingId);
        
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        
        if (booking.getStatus() != TrainerBooking.BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }
        
        // Final conflict check before confirmation
        CreateBookingRequest conflictRequest = CreateBookingRequest.builder()
            .trainerId(booking.getTrainer().getId())
            .bookingDate(booking.getBookingDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .excludeBookingId(bookingId)
            .build();
        
        ConflictCheckResult conflictCheck = conflictDetectionService.validateBookingRequest(conflictRequest);
        
        if (conflictCheck.isHasConflict()) {
            throw new BookingConflictException("Cannot confirm due to scheduling conflict", conflictCheck);
        }
        
        // Confirm booking
        booking.setStatus(TrainerBooking.BookingStatus.CONFIRMED);
        booking.setTrainerNotes(trainerNotes);
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send confirmation notifications
        notificationService.notifyClientBookingConfirmed(saved);
        
        // Evict cache for trainer availability
        evictTrainerAvailabilityCache(booking.getTrainer().getId(), booking.getBookingDate());
        
        log.info("Successfully confirmed booking {}", bookingId);
        return mapToBookingResponse(saved);
    }
    
    @Override
    public TrainerBookingResponse rejectBooking(Long bookingId, String rejectionReason) {
        log.info("Rejecting booking {}", bookingId);
        
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        
        if (booking.getStatus() != TrainerBooking.BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be rejected");
        }
        
        booking.setStatus(TrainerBooking.BookingStatus.CANCELLED);
        booking.setCancellationReason(rejectionReason);
        booking.setCancelledAt(LocalDateTime.now());
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send rejection notification
        notificationService.notifyClientBookingRejected(saved);
        
        log.info("Successfully rejected booking {}", bookingId);
        return mapToBookingResponse(saved);
    }
    
    @Override
    public TrainerBookingResponse rescheduleBooking(Long bookingId, LocalDate newDate, LocalTime newStartTime) {
        log.info("Rescheduling booking {} to {} at {}", bookingId, newDate, newStartTime);
        
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        
        if (!booking.canReschedule()) {
            throw new IllegalStateException("Booking cannot be rescheduled");
        }
        
        // Calculate new end time based on original duration
        long durationMinutes = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        LocalTime newEndTime = newStartTime.plusMinutes(durationMinutes);
        
        // Check for conflicts at new time
        CreateBookingRequest conflictRequest = CreateBookingRequest.builder()
            .trainerId(booking.getTrainer().getId())
            .bookingDate(newDate)
            .startTime(newStartTime)
            .endTime(newEndTime)
            .excludeBookingId(bookingId)
            .build();
        
        ConflictCheckResult conflictCheck = conflictDetectionService.validateBookingRequest(conflictRequest);
        
        if (conflictCheck.isHasConflict()) {
            throw new BookingConflictException("Cannot reschedule due to scheduling conflict", conflictCheck);
        }
        
        // Update booking
        LocalDate oldDate = booking.getBookingDate();
        booking.setBookingDate(newDate);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setStatus(TrainerBooking.BookingStatus.RESCHEDULED);
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send rescheduling notifications
        notificationService.notifyBookingRescheduled(saved);
        
        // Evict cache for both old and new dates
        evictTrainerAvailabilityCache(booking.getTrainer().getId(), oldDate);
        evictTrainerAvailabilityCache(booking.getTrainer().getId(), newDate);
        
        log.info("Successfully rescheduled booking {}", bookingId);
        return mapToBookingResponse(saved);
    }
    
    @Override
    public TrainerBookingResponse completeSession(Long bookingId, String clientFeedback, 
                                                Integer rating, String trainerNotes) {
        log.info("Completing session for booking {}", bookingId);
        
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        
        if (booking.getStatus() != TrainerBooking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be completed");
        }
        
        // Validate rating
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        booking.setStatus(TrainerBooking.BookingStatus.COMPLETED);
        booking.setClientFeedback(clientFeedback);
        booking.setRating(rating);
        if (trainerNotes != null) {
            booking.setTrainerNotes(trainerNotes);
        }
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send completion notifications
        notificationService.notifySessionCompleted(saved);
        
        log.info("Successfully completed session for booking {}", bookingId);
        return mapToBookingResponse(saved);
    }
    
    @Override
    public TrainerBookingResponse cancelBooking(Long bookingId, String cancellationReason) {
        log.info("Cancelling booking {}", bookingId);
        
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        
        if (!booking.canCancel()) {
            throw new IllegalStateException("Booking cannot be cancelled");
        }
        
        booking.setStatus(TrainerBooking.BookingStatus.CANCELLED);
        booking.setCancellationReason(cancellationReason);
        booking.setCancelledAt(LocalDateTime.now());
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send cancellation notifications
        notificationService.notifyBookingCancelled(saved);
        
        // Evict cache for trainer availability
        evictTrainerAvailabilityCache(booking.getTrainer().getId(), booking.getBookingDate());
        
        log.info("Successfully cancelled booking {}", bookingId);
        return mapToBookingResponse(saved);
    }
    
    @Override
    public TrainerBookingResponse markNoShow(Long bookingId, String notes) {
        log.info("Marking booking {} as no-show", bookingId);
        
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        
        if (booking.getStatus() != TrainerBooking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be marked as no-show");
        }
        
        booking.setStatus(TrainerBooking.BookingStatus.NO_SHOW);
        booking.setTrainerNotes(notes);
        
        TrainerBooking saved = trainerBookingRepository.save(booking);
        
        // Send no-show notification
        notificationService.notifyNoShow(saved);
        
        log.info("Successfully marked booking {} as no-show", bookingId);
        return mapToBookingResponse(saved);
    }
    
    @Override
    public BookingStatistics getBookingStatistics(Long trainerId, LocalDate fromDate, LocalDate toDate) {
        log.debug("Getting booking statistics for trainer {} from {} to {}", trainerId, fromDate, toDate);
        
        // Get booking counts by status
        Long totalBookings = trainerBookingRepository.countByTrainerAndDateRange(
            trainerId, fromDate, toDate);
        
        Long completedBookings = trainerBookingRepository.countByTrainerAndDateRangeAndStatus(
            trainerId, fromDate, toDate, TrainerBooking.BookingStatus.COMPLETED);
        
        Long cancelledBookings = trainerBookingRepository.countByTrainerAndDateRangeAndStatus(
            trainerId, fromDate, toDate, TrainerBooking.BookingStatus.CANCELLED);
        
        Long noShowBookings = trainerBookingRepository.countByTrainerAndDateRangeAndStatus(
            trainerId, fromDate, toDate, TrainerBooking.BookingStatus.NO_SHOW);
        
        Long pendingBookings = trainerBookingRepository.countByTrainerAndDateRangeAndStatus(
            trainerId, fromDate, toDate, TrainerBooking.BookingStatus.PENDING);
        
        // Get rating statistics
        Double averageRating = trainerBookingRepository.findAverageRatingByTrainer(trainerId);
        
        // Calculate rates
        Double completionRate = totalBookings > 0 ? 
            (completedBookings.doubleValue() / totalBookings.doubleValue()) : 0.0;
        
        Double noShowRate = totalBookings > 0 ? 
            (noShowBookings.doubleValue() / totalBookings.doubleValue()) : 0.0;
        
        Double cancellationRate = totalBookings > 0 ? 
            (cancelledBookings.doubleValue() / totalBookings.doubleValue()) : 0.0;
        
        // Get monthly breakdown
        Map<String, Long> monthlyBookings = getMonthlyBookingBreakdown(trainerId, fromDate, toDate);
        
        return BookingStatistics.builder()
            .trainerId(trainerId)
            .fromDate(fromDate)
            .toDate(toDate)
            .totalBookings(totalBookings)
            .completedBookings(completedBookings)
            .cancelledBookings(cancelledBookings)
            .noShowBookings(noShowBookings)
            .pendingBookings(pendingBookings)
            .averageRating(averageRating)
            .completionRate(completionRate)
            .noShowRate(noShowRate)
            .cancellationRate(cancellationRate)
            .monthlyBookings(monthlyBookings)
            .build();
    }
    
    @Override
    public List<TrainerBookingResponse> getPendingBookings(Long trainerId) {
        log.debug("Getting pending bookings for trainer {}", trainerId);
        
        List<TrainerBooking> pendingBookings = trainerBookingRepository
            .findPendingBookingsWithServiceInfo(trainerId);
        
        return pendingBookings.stream()
            .map(this::mapToBookingResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TrainerBookingResponse> getUpcomingBookings(Long userId) {
        log.debug("Getting upcoming bookings for user {}", userId);
        
        List<TrainerBooking> upcomingBookings = trainerBookingRepository
            .findByTrainerIdAndDateRangeAndStatuses(
                userId,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                List.of(TrainerBooking.BookingStatus.CONFIRMED, TrainerBooking.BookingStatus.PENDING)
            );
        
        return upcomingBookings.stream()
            .map(this::mapToBookingResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TrainerBookingResponse> getBookingHistory(Long userId, int page, int size) {
        log.debug("Getting booking history for user {} (page {}, size {})", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("bookingDate").descending());
        
        // This would need a pageable version of the repository method
        // For now, we'll get all and limit manually
        List<TrainerBooking> allBookings = trainerBookingRepository
            .findByTrainerIdAndDateRangeAndStatuses(
                userId,
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                List.of(TrainerBooking.BookingStatus.COMPLETED, 
                       TrainerBooking.BookingStatus.CANCELLED,
                       TrainerBooking.BookingStatus.NO_SHOW)
            );
        
        return allBookings.stream()
            .skip((long) page * size)
            .limit(size)
            .map(this::mapToBookingResponse)
            .collect(Collectors.toList());
    }
    
    private String generateBookingId() {
        return "TB" + System.currentTimeMillis();
    }
    
    private TrainerBookingResponse mapToBookingResponse(TrainerBooking booking) {
        return trainerMapper.toTrainerBookingResponse(booking);
    }
    
    private void evictTrainerAvailabilityCache(Long trainerId, LocalDate date) {
        // Implementation would depend on your caching strategy
        // For now, just log
        log.debug("Evicting trainer availability cache for trainer {} on {}", trainerId, date);
    }
    
    private Map<String, Long> getMonthlyBookingBreakdown(Long trainerId, LocalDate fromDate, LocalDate toDate) {
        // This would need a more sophisticated query
        // For now, return empty map
        return new HashMap<>();
    }
}