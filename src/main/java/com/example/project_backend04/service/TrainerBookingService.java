package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerBooking.CreateTrainerBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerAvailabilityResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerBookingService {
    
    private final TrainerBookingRepository trainerBookingRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public TrainerBookingResponse createBooking(Long userId, CreateTrainerBookingRequest request) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate trainer exists and is a trainer
        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        if (!trainer.isTrainer()) {
            throw new RuntimeException("Selected user is not a trainer");
        }
        
        // Validate booking date is in the future
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot book sessions in the past");
        }
        
        // Validate time range
        if (request.getStartTime().isAfter(request.getEndTime()) || 
            request.getStartTime().equals(request.getEndTime())) {
            throw new RuntimeException("Invalid time range");
        }
        
        // Check for conflicts
        List<TrainerBooking> conflicts = trainerBookingRepository.findConflictingBookings(
                trainer, request.getBookingDate(), request.getStartTime(), request.getEndTime());
        
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Trainer is not available at the selected time");
        }
        
        // Check if user already has booking with this trainer on the same date
        trainerBookingRepository.findActiveBookingByUserAndTrainerAndDate(
                user, trainer, request.getBookingDate())
                .ifPresent(existing -> {
                    throw new RuntimeException("You already have a booking with this trainer on this date");
                });
        
        // Create booking
        TrainerBooking booking = new TrainerBooking();
        booking.setUser(user);
        booking.setTrainer(trainer);
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setNotes(request.getNotes());
        booking.setSessionType(request.getSessionType());
        
        TrainerBooking savedBooking = trainerBookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }
    
    public List<TrainerBookingResponse> getUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<TrainerBooking> bookings = trainerBookingRepository.findByUserOrderByBookingDateDescStartTimeDesc(user);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public List<TrainerBookingResponse> getTrainerBookings(Long trainerId) {
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        if (!trainer.isTrainer()) {
            throw new RuntimeException("User is not a trainer");
        }
        
        List<TrainerBooking> bookings = trainerBookingRepository.findByTrainerOrderByBookingDateDescStartTimeDesc(trainer);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public List<TrainerBookingResponse> getUpcomingUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<TrainerBooking> bookings = trainerBookingRepository.findUpcomingBookingsByUser(user, LocalDate.now());
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public List<TrainerBookingResponse> getUpcomingTrainerBookings(Long trainerId) {
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        if (!trainer.isTrainer()) {
            throw new RuntimeException("User is not a trainer");
        }
        
        List<TrainerBooking> bookings = trainerBookingRepository.findUpcomingBookingsByTrainer(trainer, LocalDate.now());
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public TrainerAvailabilityResponse getTrainerAvailability(Long trainerId, LocalDate date) {
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        if (!trainer.isTrainer()) {
            throw new RuntimeException("User is not a trainer");
        }
        
        List<TrainerBooking> bookings = trainerBookingRepository.findTrainerBookingsForDate(trainer, date);
        
        TrainerAvailabilityResponse response = new TrainerAvailabilityResponse();
        response.setTrainerId(trainerId);
        response.setTrainerName(trainer.getFullName());
        response.setDate(date);
        
        // Convert bookings to booked slots
        List<TrainerAvailabilityResponse.TimeSlot> bookedSlots = bookings.stream()
                .map(booking -> {
                    TrainerAvailabilityResponse.TimeSlot slot = new TrainerAvailabilityResponse.TimeSlot();
                    slot.setStartTime(booking.getStartTime());
                    slot.setEndTime(booking.getEndTime());
                    slot.setBookingId(booking.getBookingId());
                    slot.setClientName(booking.getUser().getFullName());
                    return slot;
                })
                .collect(Collectors.toList());
        
        response.setBookedSlots(bookedSlots);
        
        // Generate available slots (example: 8 AM to 8 PM, 1-hour slots)
        List<TrainerAvailabilityResponse.TimeSlot> availableSlots = generateAvailableSlots(bookings, date);
        response.setAvailableSlots(availableSlots);
        
        return response;
    }
    
    @Transactional
    public TrainerBookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        TrainerBooking booking = trainerBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Check if user owns this booking or is the trainer
        if (!booking.getUser().getId().equals(userId) && !booking.getTrainer().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to cancel this booking");
        }
        
        if (!booking.canCancel()) {
            throw new RuntimeException("Cannot cancel this booking (too close to session time or already completed)");
        }
        
        booking.setStatus(TrainerBooking.BookingStatus.CANCELLED);
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancellationReason(reason);
        
        TrainerBooking savedBooking = trainerBookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }
    
    private List<TrainerAvailabilityResponse.TimeSlot> generateAvailableSlots(List<TrainerBooking> bookings, LocalDate date) {
        List<TrainerAvailabilityResponse.TimeSlot> availableSlots = new ArrayList<>();
        
        // Working hours: 8 AM to 8 PM
        LocalTime startHour = LocalTime.of(8, 0);
        LocalTime endHour = LocalTime.of(20, 0);
        
        LocalTime currentTime = startHour;
        while (currentTime.isBefore(endHour)) {
            final LocalTime slotStart = currentTime;
            final LocalTime slotEnd = currentTime.plusHours(1);
            final LocalDate checkDate = date;
            
            // Check if this slot conflicts with any booking
            boolean isAvailable = bookings.stream().noneMatch(booking -> 
                booking.hasTimeConflict(checkDate, slotStart, slotEnd));
            
            if (isAvailable) {
                TrainerAvailabilityResponse.TimeSlot slot = new TrainerAvailabilityResponse.TimeSlot();
                slot.setStartTime(slotStart);
                slot.setEndTime(slotEnd);
                availableSlots.add(slot);
            }
            
            currentTime = slotEnd;
        }
        
        return availableSlots;
    }
    
    private TrainerBookingResponse mapToResponse(TrainerBooking booking) {
        TrainerBookingResponse response = new TrainerBookingResponse();
        response.setId(booking.getId());
        response.setBookingId(booking.getBookingId());
        
        // Map user and trainer as UserResponse objects
        UserResponse userResponse = new UserResponse();
        userResponse.setId(booking.getUser().getId());
        userResponse.setFullName(booking.getUser().getFullName());
        userResponse.setEmail(booking.getUser().getEmail());
        userResponse.setAvatar(booking.getUser().getAvatar());
        response.setUser(userResponse);
        
        UserResponse trainerResponse = new UserResponse();
        trainerResponse.setId(booking.getTrainer().getId());
        trainerResponse.setFullName(booking.getTrainer().getFullName());
        trainerResponse.setEmail(booking.getTrainer().getEmail());
        trainerResponse.setAvatar(booking.getTrainer().getAvatar());
        response.setTrainer(trainerResponse);
        
        response.setBookingDate(booking.getBookingDate());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setNotes(booking.getNotes());
        response.setSessionType(booking.getSessionType());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        
        // Service integration fields
        response.setServiceRegistrationId(booking.getServiceRegistrationId());
        response.setSessionObjective(booking.getSessionObjective());
        response.setSessionNumber(booking.getSessionNumber());
        response.setTrainerNotes(booking.getTrainerNotes());
        response.setClientFeedback(booking.getClientFeedback());
        response.setRating(booking.getRating());
        
        return response;
    }
}