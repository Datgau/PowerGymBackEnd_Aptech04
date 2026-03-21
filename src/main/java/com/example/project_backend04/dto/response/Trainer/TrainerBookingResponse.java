package com.example.project_backend04.dto.response.Trainer;

import com.example.project_backend04.entity.TrainerBooking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerBookingResponse {
    
    // Booking Basic Info
    private Long id;
    private String bookingId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private TrainerBooking.BookingStatus status;
    private String notes;
    private String specialRequests;
    private Integer rating;
    private String feedback;
    
    // Client Information
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String clientAvatar;
    
    // Trainer Information
    private String trainerName;
    private String trainerEmail;
    private String trainerPhone;
    private String trainerAvatar;
    
    // Service Information
    private String serviceName;
    private String serviceDescription;
    private Double servicePrice;
    private String serviceCategory;
    private boolean isServiceLinked;
    private Long serviceRegistrationId;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    
    // Additional Info
    private String cancellationReason;
    private String rejectionReason;
    private boolean canReschedule;
    private boolean canCancel;
    private boolean canRate;
    
    // Helper methods
    public boolean isPending() {
        return status == TrainerBooking.BookingStatus.PENDING;
    }
    
    public boolean isConfirmed() {
        return status == TrainerBooking.BookingStatus.CONFIRMED;
    }
    
    public boolean isCompleted() {
        return status == TrainerBooking.BookingStatus.COMPLETED;
    }
    
    public boolean isCancelled() {
        return status == TrainerBooking.BookingStatus.CANCELLED;
    }
    
    public boolean isRejected() {
        return status == TrainerBooking.BookingStatus.CANCELLED;
    }
    
    public boolean isUpcoming() {
        return isConfirmed() && bookingDate.isAfter(LocalDate.now());
    }
    
    public boolean isPast() {
        return bookingDate.isBefore(LocalDate.now()) || 
               (bookingDate.equals(LocalDate.now()) && endTime.isBefore(LocalTime.now()));
    }
    
    public boolean isToday() {
        return bookingDate.equals(LocalDate.now());
    }
    
    public int getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }
    
    public String getFormattedDuration() {
        int minutes = getDurationMinutes();
        if (minutes < 60) {
            return minutes + " phút";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " giờ";
            } else {
                return hours + " giờ " + remainingMinutes + " phút";
            }
        }
    }
    
    public String getStatusDisplay() {
        switch (status) {
            case PENDING:
                return "Chờ xác nhận";
            case CONFIRMED:
                return "Đã xác nhận";
            case COMPLETED:
                return "Hoàn thành";
            case CANCELLED:
                return "Đã hủy";
            case RESCHEDULED:
                return "Đã dời lịch";
            default:
                return status.name();
        }
    }
    
    public String getTimeSlot() {
        if (startTime != null && endTime != null) {
            return startTime.toString() + " - " + endTime.toString();
        }
        return "";
    }
}