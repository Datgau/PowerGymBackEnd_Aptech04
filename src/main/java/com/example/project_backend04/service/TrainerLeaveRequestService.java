package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerLeave.LeaveRequestCreateRequest;
import com.example.project_backend04.dto.request.TrainerLeave.LeaveRequestReviewRequest;
import com.example.project_backend04.dto.response.TrainerLeave.LeaveRequestResponse;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.TrainerLeaveRequest;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.LeaveRequestStatus;
import com.example.project_backend04.enums.LeaveRequestType;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.TrainerLeaveRequestRepository;
import com.example.project_backend04.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainerLeaveRequestService {
    
    private final TrainerLeaveRequestRepository leaveRequestRepository;
    private final TrainerBookingRepository trainerBookingRepository;
    private final UserRepository userRepository;
    
    // Time slots for full day leave (8:00 - 22:00, every 2 hours)
    private static final List<TimeSlot> FULL_DAY_SLOTS = List.of(
        new TimeSlot(LocalTime.of(8, 0), LocalTime.of(10, 0)),
        new TimeSlot(LocalTime.of(10, 0), LocalTime.of(12, 0)),
        new TimeSlot(LocalTime.of(12, 0), LocalTime.of(14, 0)),
        new TimeSlot(LocalTime.of(14, 0), LocalTime.of(16, 0)),
        new TimeSlot(LocalTime.of(16, 0), LocalTime.of(18, 0)),
        new TimeSlot(LocalTime.of(18, 0), LocalTime.of(20, 0)),
        new TimeSlot(LocalTime.of(20, 0), LocalTime.of(22, 0))
    );
    
    /**
     * Create a new leave request
     */
    public LeaveRequestResponse createLeaveRequest(Long trainerId, LeaveRequestCreateRequest request) {
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        if (!"TRAINER".equals(trainer.getRole().getName())) {
            throw new RuntimeException("User is not a trainer");
        }
        
        // Validate date is in the future
        if (request.getLeaveDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot request leave for past dates");
        }
        
        // Validate time slot type
        if (request.getLeaveType() == LeaveRequestType.TIME_SLOT) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new RuntimeException("Start time and end time are required for time slot leave");
            }
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new RuntimeException("Start time must be before end time");
            }
        }
        
        // Check for existing approved leave on the same date/time
        boolean hasConflict = leaveRequestRepository.hasApprovedLeaveOnDateTime(
            trainerId,
            request.getLeaveDate(),
            request.getStartTime() != null ? request.getStartTime() : LocalTime.of(0, 0),
            request.getEndTime() != null ? request.getEndTime() : LocalTime.of(23, 59)
        );
        
        if (hasConflict) {
            throw new RuntimeException("You already have an approved leave request for this date/time");
        }
        
        TrainerLeaveRequest leaveRequest = TrainerLeaveRequest.builder()
            .trainer(trainer)
            .leaveDate(request.getLeaveDate())
            .leaveType(request.getLeaveType())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .reason(request.getReason())
            .status(LeaveRequestStatus.PENDING)
            .build();
        
        TrainerLeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Created leave request {} for trainer {}", saved.getId(), trainerId);
        
        return mapToResponse(saved);
    }
    
    /**
     * Get all leave requests for a trainer
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getTrainerLeaveRequests(Long trainerId) {
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        List<TrainerLeaveRequest> requests = leaveRequestRepository.findByTrainerOrderByCreatedAtDesc(trainer);
        return requests.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all pending leave requests (for admin)
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getAllPendingLeaveRequests() {
        List<TrainerLeaveRequest> requests = leaveRequestRepository.findByStatusOrderByCreatedAtAsc(LeaveRequestStatus.PENDING);
        return requests.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Review (approve/reject) a leave request
     */
    public LeaveRequestResponse reviewLeaveRequest(Long requestId, Long adminId, LeaveRequestReviewRequest request) {
        TrainerLeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new RuntimeException("Leave request has already been reviewed");
        }
        
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        leaveRequest.setStatus(request.getStatus());
        leaveRequest.setAdminNotes(request.getAdminNotes());
        leaveRequest.setReviewedBy(admin);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        
        TrainerLeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        
        // If approved, create blocking bookings
        if (request.getStatus() == LeaveRequestStatus.APPROVED) {
            createBlockingBookings(leaveRequest);
            log.info("Approved leave request {} and created blocking bookings", requestId);
        } else {
            log.info("Rejected leave request {}", requestId);
        }
        
        return mapToResponse(updated);
    }
    
    /**
     * Delete a leave request (only if pending)
     */
    public void deleteLeaveRequest(Long requestId, Long trainerId) {
        TrainerLeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        if (!leaveRequest.getTrainer().getId().equals(trainerId)) {
            throw new RuntimeException("You can only delete your own leave requests");
        }
        
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new RuntimeException("Can only delete pending leave requests");
        }
        
        leaveRequestRepository.delete(leaveRequest);
        log.info("Deleted leave request {}", requestId);
    }
    
    /**
     * Create blocking bookings for approved leave
     */
    private void createBlockingBookings(TrainerLeaveRequest leaveRequest) {
        List<TrainerBooking> blockingBookings = new ArrayList<>();
        
        if (leaveRequest.getLeaveType() == LeaveRequestType.FULL_DAY) {
            // Create bookings for all time slots
            for (TimeSlot slot : FULL_DAY_SLOTS) {
                TrainerBooking booking = createBlockingBooking(
                    leaveRequest.getTrainer(),
                    leaveRequest.getLeaveDate(),
                    slot.start,
                    slot.end,
                    "Trainer Leave: " + (leaveRequest.getReason() != null ? leaveRequest.getReason() : "Day off")
                );
                blockingBookings.add(booking);
            }
        } else {
            // Create booking for specific time slot
            TrainerBooking booking = createBlockingBooking(
                leaveRequest.getTrainer(),
                leaveRequest.getLeaveDate(),
                leaveRequest.getStartTime(),
                leaveRequest.getEndTime(),
                "Trainer Leave: " + (leaveRequest.getReason() != null ? leaveRequest.getReason() : "Busy")
            );
            blockingBookings.add(booking);
        }
        
        trainerBookingRepository.saveAll(blockingBookings);
        log.info("Created {} blocking bookings for leave request {}", blockingBookings.size(), leaveRequest.getId());
    }
    
    /**
     * Create a blocking booking
     */
    private TrainerBooking createBlockingBooking(User trainer, LocalDate date, LocalTime start, LocalTime end, String notes) {
        return TrainerBooking.builder()
            .trainer(trainer)
            .user(trainer) // Self-booking
            .bookingDate(date)
            .startTime(start)
            .endTime(end)
            .status(BookingStatus.CONFIRMED) // Use CONFIRMED to block the slot
            .notes(notes)
            .isAssignedByAdmin(true)
            .build();
    }
    
    /**
     * Map entity to response DTO
     */
    private LeaveRequestResponse mapToResponse(TrainerLeaveRequest request) {
        return LeaveRequestResponse.builder()
            .id(request.getId())
            .trainerId(request.getTrainer().getId())
            .trainerName(request.getTrainer().getFullName())
            .trainerEmail(request.getTrainer().getEmail())
            .trainerAvatar(request.getTrainer().getAvatar())
            .leaveDate(request.getLeaveDate())
            .leaveType(request.getLeaveType())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .reason(request.getReason())
            .status(request.getStatus())
            .adminNotes(request.getAdminNotes())
            .reviewedBy(request.getReviewedBy() != null ? request.getReviewedBy().getId() : null)
            .reviewedByName(request.getReviewedBy() != null ? request.getReviewedBy().getFullName() : null)
            .reviewedAt(request.getReviewedAt())
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .build();
    }
    
    /**
     * Helper class for time slots
     */
    private static class TimeSlot {
        LocalTime start;
        LocalTime end;
        
        TimeSlot(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
