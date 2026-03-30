package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
import com.example.project_backend04.dto.request.TrainerBooking.RejectBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ITrainerBookingService;
import com.example.project_backend04.service.IService.ITrainerWorkingHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainerBookingServiceImpl implements ITrainerBookingService {

    private final TrainerBookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ServiceRegistrationRepository serviceRegRepo;
    private final TrainerSpecialtyRepository specialtyRepo;
    private final ITrainerWorkingHoursService workingHoursService;


    @Override
    public TrainerBookingResponse createBooking(Long userId, CreateBookingRequest req) {

        // 1. Load user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. Load & validate ServiceRegistration
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



    // ── Trainer actions ───────────────────────────────────────────────────────────

    @Override
    public TrainerBookingResponse acceptBooking(Long trainerId, Long bookingId, String trainerNotes) {
        TrainerBooking booking = findAndValidateTrainer(bookingId, trainerId);

        if (!booking.canTrainerRespond()) {
            throw new IllegalStateException(
                    "Không thể xác nhận: booking không ở trạng thái PENDING");
        }

        // Race-condition guard: kiểm tra lại conflict trước khi confirm
        List<TrainerBooking> conflicts = bookingRepo.findConflictingBookingsExcluding(
                trainerId, booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), bookingId);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "Có lịch booking khác đã được xác nhận trong khung giờ này");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTrainerNotes(trainerNotes);

        log.info("Trainer {} accepted booking {}", trainerId, booking.getBookingId());
        return toResponse(bookingRepo.save(booking));
    }

    @Override
    public TrainerBookingResponse rejectBooking(Long trainerId, Long bookingId,
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



    private TrainerBooking findAndValidateOwner(Long bookingId, Long userId) {
        TrainerBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền thao tác với booking này");
        }
        return booking;
    }

    private TrainerBooking findAndValidateTrainer(Long bookingId, Long trainerId) {
        TrainerBooking booking = bookingRepo.findById(bookingId)
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
