package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.Trainer.TrainerBookingResponse;
import com.example.project_backend04.entity.TrainerBooking;

public class TrainerBookingMapper {

    public static TrainerBookingResponse toResponse(TrainerBooking booking) {

        if (booking == null) return null;

        var user = booking.getUser();
        var trainer = booking.getTrainer();
        var serviceReg = booking.getServiceRegistration();
        var gymService = serviceReg != null ? serviceReg.getGymService() : null;

        return TrainerBookingResponse.builder()

                // ===== BASIC =====
                .id(booking.getId())
                .bookingId(booking.getBookingId())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .notes(booking.getNotes())
                .specialRequests(booking.getSessionObjective())
                .rating(booking.getRating())
                .feedback(booking.getClientFeedback())

                // ===== CLIENT =====
                .clientName(user != null ? user.getFullName() : null)
                .clientEmail(user != null ? user.getEmail() : null)
                .clientPhone(user != null ? user.getPhoneNumber() : null)
                .clientAvatar(user != null ? user.getAvatar() : null)

                // ===== TRAINER =====
                .trainerName(trainer != null ? trainer.getFullName() : null)
                .trainerEmail(trainer != null ? trainer.getEmail() : null)
                .trainerPhone(trainer != null ? trainer.getPhoneNumber() : null)
                .trainerAvatar(trainer != null ? trainer.getAvatar() : null)

                // ===== SERVICE =====
                .serviceName(booking.getServiceName())
                .serviceRegistrationId(booking.getServiceRegistrationId())
                .isServiceLinked(booking.isLinkedToService())

                .serviceDescription(gymService != null ? gymService.getDescription() : null)
                .servicePrice(gymService != null && gymService.getPrice() != null
                        ? gymService.getPrice().doubleValue()
                        : null)
                .serviceCategory(gymService != null && gymService.getCategory() != null
                        ? gymService.getCategory().getName()
                        : null)

                // ===== TIMESTAMPS =====
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .cancelledAt(booking.getCancelledAt())

                .confirmedAt(null)   // entity chưa có
                .completedAt(null)   // entity chưa có

                // ===== EXTRA =====
                .cancellationReason(booking.getCancellationReason())
                .rejectionReason(booking.getRejectionReason())

                // ===== LOGIC =====
                .canCancel(booking.canCancel())
                .canReschedule(booking.canReschedule())
                .canRate(booking.isCompleted() && !booking.hasRating())
                
                // ===== PAYMENT =====
                .paymentStatus(booking.getPaymentStatus())

                .build();
    }
}