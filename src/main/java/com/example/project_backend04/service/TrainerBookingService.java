package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
import com.example.project_backend04.dto.request.TrainerBooking.RejectBookingRequest;
import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.repository.*;
import com.example.project_backend04.service.IService.ITrainerBookingService;
import com.example.project_backend04.service.IService.ITrainerWorkingHoursService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainerBookingService implements ITrainerBookingService {

    private final TrainerBookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ServiceRegistrationRepository serviceRegRepo;
    private final TrainerSpecialtyRepository specialtyRepo;
    private final ITrainerWorkingHoursService workingHoursService;
    private final PaymentOrderRepository paymentOrderRepo;
    private final GymServiceRepository gymServiceRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;

    @Override
    public TrainerBookingResponse createBooking(Long userId, CreateBookingRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        ServiceRegistration reg = serviceRegRepo.findById(req.getServiceRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ServiceRegistration not found: " + req.getServiceRegistrationId()));

        if (!reg.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This ServiceRegistration does not belong to the user");
        }

        // Registration must be ACTIVE
        if (!"ACTIVE".equals(reg.getStatus() != null ? reg.getStatus().name() : "")) {
            throw new IllegalStateException(
                    "ServiceRegistration is not ACTIVE (status: " + reg.getStatus() + ")");
        }

        // 3. Validate time range
        if (!req.isValidTimeRange()) {
            throw new IllegalArgumentException("Invalid time range: startTime must be before endTime");
        }
        if (!req.isMinimumDuration()) {
            throw new IllegalArgumentException("Minimum session duration is 30 minutes");
        }
        if (!req.isMaximumDuration()) {
            throw new IllegalArgumentException("Maximum session duration is 8 hours");
        }

        // Check user booking conflicts
        // First, check if user already has a PENDING/CONFIRMED booking for this service registration
        List<TrainerBooking> existingBookings = bookingRepo.findByServiceRegistration_Id(req.getServiceRegistrationId())
            .stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
            .filter(b -> b.getUser().getId().equals(userId))
            .collect(Collectors.toList());
        
        if (!existingBookings.isEmpty()) {
            TrainerBooking existingBooking = existingBookings.get(0);
            log.info("User {} already has booking {} for service registration {}, returning existing booking",
                    userId, existingBooking.getBookingId(), req.getServiceRegistrationId());
            return toResponse(existingBooking);
        }
        
        // Check for time slot conflicts with OTHER bookings
        List<TrainerBooking> userConflicts = bookingRepo.findConflictingBookingsForUser(
                userId, req.getBookingDate(), req.getStartTime(), req.getEndTime());

        if (!userConflicts.isEmpty()) {
            throw new IllegalStateException("You already have a booking in this time slot");
        }

        User trainer = null;

        if (req.getTrainerId() != null) {
            trainer = userRepo.findById(req.getTrainerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Trainer not found: " + req.getTrainerId()));

            // Validate trainer specialty matches service category
            Long categoryId = reg.getGymService().getCategory().getId();
            boolean hasSpecialty = specialtyRepo.hasTrainerSpecialtyForCategory(
                    req.getTrainerId(), categoryId);

            if (!hasSpecialty) {
                throw new IllegalArgumentException(
                        "Trainer does not have the required specialty for this service");
            }

            // Check trainer schedule conflicts
            List<TrainerBooking> trainerConflicts =
                    bookingRepo.findConflictingBookingsForTrainer(
                            req.getTrainerId(),
                            req.getBookingDate(),
                            req.getStartTime(),
                            req.getEndTime(),
                            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                    );

            if (!trainerConflicts.isEmpty()) {
                throw new IllegalStateException("Trainer is already booked for this time slot");
            }

            // Validate trainer working hours
            if (!workingHoursService.isWithinWorkingHours(
                    req.getTrainerId(),
                    req.getBookingDate(),
                    req.getStartTime(),
                    req.getEndTime())) {

                throw new IllegalStateException(
                        "Selected time is outside the trainer's working hours");
            }
        }

        // 6. Create booking
        TrainerBooking booking = TrainerBooking.builder()
                .user(user)
                .trainer(trainer)
                .serviceRegistration(reg)
                .bookingDate(req.getBookingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .notes(req.getNotes())
                .sessionType(req.getSessionType())
                .status(BookingStatus.PENDING)
                .isAssignedByAdmin(false)
                .build();

        TrainerBooking saved = bookingRepo.save(booking);
        
        // 7. Link to existing PaymentOrder if exists
        // PaymentOrder should already exist from payment flow
        // For ONLINE registrations, find SUCCESS payment (already paid)
        // For COUNTER registrations, find PENDING payment (will pay at counter)
        Optional<PaymentOrder> existingPaymentOrder = Optional.empty();
        
        if (reg.getRegistrationType() == com.example.project_backend04.enums.RegistrationType.ONLINE) {
            // Online registration - find SUCCESS payment
            existingPaymentOrder = paymentOrderRepo.findByUserAndItemIdAndStatus(
                user, reg.getGymService().getId().toString(), PaymentStatus.SUCCESS);
            
            if (existingPaymentOrder.isEmpty()) {
                log.warn("No SUCCESS PaymentOrder found for ONLINE booking {} - checking PENDING", saved.getBookingId());
                // Fallback to PENDING if SUCCESS not found (edge case)
                existingPaymentOrder = paymentOrderRepo.findByUserAndItemIdAndStatus(
                    user, reg.getGymService().getId().toString(), PaymentStatus.PENDING);
            }
        } else {
            // Counter registration - find PENDING payment (will pay at counter)
            existingPaymentOrder = paymentOrderRepo.findByUserAndItemIdAndStatus(
                user, reg.getGymService().getId().toString(), PaymentStatus.PENDING);
        }
        
        if (existingPaymentOrder.isPresent()) {
            PaymentOrder paymentOrder = existingPaymentOrder.get();
            saved.setPaymentOrder(paymentOrder);
            saved = bookingRepo.save(saved);
            log.info("Linked PaymentOrder {} (status: {}) to booking {}", 
                paymentOrder.getId(), paymentOrder.getStatus(), saved.getBookingId());
        } else {
            log.warn("No PaymentOrder found for booking {} - user may need to pay", saved.getBookingId());
        }
        
        // 8. Update ServiceRegistration với trainer nếu chưa có
        if (trainer != null && reg.getTrainer() == null) {
            reg.setTrainer(trainer);
            reg.setTrainerSelectedAt(java.time.LocalDateTime.now());
            serviceRegRepo.save(reg);
            log.info("Updated ServiceRegistration {} with trainer {}", reg.getId(), trainer.getId());
        }

        log.info("Created booking {} for user {} with trainer {}",
                saved.getBookingId(), userId, req.getTrainerId());

        return toResponse(saved);
    }
    @Override
    public TrainerBookingResponse cancelBookingByUser(Long userId, Long bookingId, String reason) {
        TrainerBooking booking = findAndValidateOwner(bookingId, userId);

        if (!booking.canCancel()) {
            throw new IllegalStateException(
                    "Không thể hủy lịch: chỉ được hủy ít nhất 2 tiếng trước buổi hẹn");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());

        log.info("User {} cancelled booking {}", userId, booking.getBookingId());
        return toResponse(bookingRepo.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerBookingResponse> getMyBookings(Long userId, String statusStr) {
        BookingStatus status = parseStatus(statusStr);
        return bookingRepo.findByUserIdAndOptionalStatus(userId, status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

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

    // ── Trainer actions ───────────────────────────────────────────────────────────

    @Override
    public TrainerBookingResponse acceptBooking(Long trainerId, String bookingId, String trainerNotes) {
        TrainerBooking booking = findAndValidateTrainer(bookingId, trainerId);

        if (!booking.canTrainerRespond()) {
            throw new IllegalStateException(
                    "Không thể xác nhận: booking không ở trạng thái PENDING");
        }

        // Race-condition guard: kiểm tra lại conflict trước khi confirm
        List<BookingStatus> statuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        List<TrainerBooking> conflicts = bookingRepo.findConflictingBookingsExcluding(
                trainerId, booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getId(), statuses);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "Có lịch booking khác đã được xác nhận trong khung giờ này");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTrainerNotes(trainerNotes);
        return toResponse(bookingRepo.save(booking));
    }

    @Override
    public TrainerBookingResponse rejectBooking(Long trainerId, String bookingId,
                                                RejectBookingRequest req) {
        TrainerBooking booking = findAndValidateTrainer(bookingId, trainerId);

        if (!booking.canTrainerRespond()) {
            throw new IllegalStateException(
                    "Không thể từ chối: booking không ở trạng thái PENDING");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(req.getRejectionReason());
        booking.setRejectionMediaUrl(req.getRejectionMediaUrl());
        booking.setRejectedAt(LocalDateTime.now());

        log.info("Trainer {} rejected booking {}: {}",
                trainerId, booking.getBookingId(), req.getRejectionReason());
        return toResponse(bookingRepo.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerBookingResponse> getPendingBookingsForTrainer(Long trainerId) {
        return bookingRepo.findPendingBookingsForTrainer(trainerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerBookingResponse> getUpcomingBookingsForTrainer(Long trainerId) {
        return bookingRepo.findUpcomingConfirmedForTrainer(trainerId, LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Admin actions ─────────────────────────────────────────────────────────────

    @Override
    public TrainerBookingResponse assignTrainerByAdmin(Long adminId, Long bookingId, Long trainerId) {
        TrainerBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking: " + bookingId));

        if (booking.getTrainer() != null) {
            throw new IllegalStateException(
                    "Booking này đã có trainer được gán (trainer: "
                    + booking.getTrainer().getFullName() + ")");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể gán trainer cho booking đang ở trạng thái PENDING");
        }

        User trainer = userRepo.findById(trainerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trainer: " + trainerId));

        // Validate specialty
        if (booking.isLinkedToService()) {
            Long categoryId = booking.getServiceRegistration().getGymService().getCategory().getId();
            if (!specialtyRepo.hasTrainerSpecialtyForCategory(trainerId, categoryId)) {
                throw new IllegalArgumentException(
                        "Trainer không có chuyên môn phù hợp với gói dịch vụ của booking này");
            }
        }

        // Validate không trùng lịch
        List<TrainerBooking> conflicts =
                bookingRepo.findConflictingBookingsForTrainer(
                        trainerId,
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                );

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Trainer đã có lịch trùng vào khung giờ này");
        }

        booking.setTrainer(trainer);
        booking.setIsAssignedByAdmin(true);

        log.info("Admin {} assigned trainer {} to booking {}", adminId, trainerId, booking.getBookingId());
        return toResponse(bookingRepo.save(booking));
    }

    @Override
    public TrainerBookingResponse cancelBookingByAdmin(Long adminId, Long bookingId, String reason) {
        TrainerBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking: " + bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể hủy booking đã hoàn thành hoặc đã hủy");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("[Admin] " + (reason != null ? reason : ""));
        booking.setCancelledAt(LocalDateTime.now());

        log.info("Admin {} cancelled booking {}", adminId, booking.getBookingId());
        return toResponse(bookingRepo.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerBookingResponse> getUnassignedBookings(Long categoryId) {
        if (categoryId != null) {
            return bookingRepo.findUnassignedPendingBookingsByCategory(categoryId)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }
        return bookingRepo.findUnassignedPendingBookings()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerBookingResponse> getAllRejectedBookings() {
        return bookingRepo.findAllRejectedBookings()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Validation helpers ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TrainerBookingResponse> getBookingsByServiceRegistration(Long serviceRegistrationId) {
        return bookingRepo.findByServiceRegistration_Id(serviceRegistrationId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserTimeSlotConflict(Long userId, LocalDate date, 
                                            LocalTime startTime, LocalTime endTime) {
        List<TrainerBooking> conflicts = bookingRepo.findConflictingBookingsForUser(
            userId, date, startTime, endTime);
        return !conflicts.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkTrainerTimeSlotConflict(Long trainerId, LocalDate date,
                                                LocalTime startTime, LocalTime endTime) {
        List<TrainerBooking> conflicts = bookingRepo.findConflictingBookingsForTrainer(
            trainerId, date, startTime, endTime,
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        return !conflicts.isEmpty();
    }



    private TrainerBooking findAndValidateOwner(Long bookingId, Long userId) {
        TrainerBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền thao tác với booking này");
        }
        return booking;
    }

    private TrainerBooking findAndValidateTrainer(String bookingId, Long trainerId) {
        TrainerBooking booking = bookingRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking: " + bookingId));
        if (booking.getTrainer() == null || !booking.getTrainer().getId().equals(trainerId)) {
            throw new SecurityException("Bạn không phải trainer được gán cho booking này");
        }
        return booking;
    }

    private BookingStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return BookingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status không hợp lệ: " + statusStr);
        }
    }

    /** Map entity → response DTO */
    private TrainerBookingResponse toResponse(TrainerBooking b) {
        return TrainerBookingResponse.builder()
                .id(b.getId())
                .bookingId(b.getBookingId())
                .user(b.getUser() != null ? toUserResponse(b.getUser()) : null)
                .trainer(b.getTrainer() != null ? toUserResponse(b.getTrainer()) : null)
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .notes(b.getNotes())
                .sessionType(b.getSessionType())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                // Rejection
                .rejectedAt(b.getRejectedAt())
                .rejectionReason(b.getRejectionReason())
                .rejectionMediaUrl(b.getRejectionMediaUrl())
                // Admin
                .isAssignedByAdmin(b.getIsAssignedByAdmin())
                // Service
                .serviceRegistrationId(b.getServiceRegistrationId())
                .serviceName(b.getServiceName())
                .trainerNotes(b.getTrainerNotes())
                .clientFeedback(b.getClientFeedback())
                .rating(b.getRating())
                // Payment
                .paymentStatus(b.getPaymentStatus())
                .build();
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .avatar(u.getAvatar())
                .build();
    }
}
