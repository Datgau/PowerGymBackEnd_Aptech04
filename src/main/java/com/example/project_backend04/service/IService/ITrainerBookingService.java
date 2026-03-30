package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
import com.example.project_backend04.dto.request.TrainerBooking.RejectBookingRequest;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;

import java.util.List;

/**
 * Interface cho luồng đặt lịch với Trainer mới:
 * - User tạo yêu cầu (có hoặc không chọn trainer)
 * - Trainer chấp nhận / từ chối (kèm lý do + media)
 * - Admin gán trainer cho booking chưa được gán
 */
public interface ITrainerBookingService {

    // ── User actions ─────────────────────────────────────────────────────────────

    /**
     * User tạo booking mới.
     * - trainerId có thể null → admin sẽ gán trainer sau.
     * - Validate: user phải có ServiceRegistration ACTIVE.
     * - Validate: trainer (nếu có) phải có chuyên môn phù hợp với gói dịch vụ.
     * - Validate: không trùng lịch trainer (PENDING/CONFIRMED) và không trùng lịch user.
     * - Validate: thời gian nằm trong working hours của trainer (nếu có).
     */
    TrainerBookingResponse createBooking(Long userId, CreateBookingRequest request);

    /**
     * User hủy booking của chính mình.
     * - Chỉ hủy được nếu ≥ 2 tiếng trước buổi hẹn.
     */
    TrainerBookingResponse cancelBookingByUser(Long userId, Long bookingId, String reason);

    /**
     * Lấy tất cả booking của user (My Bookings page).
     * Status null → lấy tất cả.
     */
    List<TrainerBookingResponse> getMyBookings(Long userId, String status);

    // ── Trainer actions ───────────────────────────────────────────────────────────

    /**
     * Trainer xác nhận (chấp nhận) booking.
     * - Chỉ xác nhận được booking ở trạng thái PENDING.
     * - Kiểm tra lại trùng lịch trước khi xác nhận (race-condition guard).
     */
    TrainerBookingResponse acceptBooking(Long trainerId, Long bookingId, String trainerNotes);

    /**
     * Trainer từ chối booking.
     * - Chỉ từ chối được booking ở trạng thái PENDING.
     * - Lý do là bắt buộc.
     * - Media (hình/file) là tùy chọn, upload qua Cloudinary trước khi gọi method này.
     */
    TrainerBookingResponse rejectBooking(Long trainerId, Long bookingId, RejectBookingRequest request);

    /**
     * Lấy tất cả PENDING booking mà trainer cần xác nhận.
     */
    List<TrainerBookingResponse> getPendingBookingsForTrainer(Long trainerId);

    /**
     * Lấy tất cả upcoming CONFIRMED booking của trainer.
     */
    List<TrainerBookingResponse> getUpcomingBookingsForTrainer(Long trainerId);

    // ── Admin actions ─────────────────────────────────────────────────────────────

    /**
     * Admin gán trainer cho booking chưa có trainer.
     * - Validate: trainer phải có chuyên môn phù hợp và không trùng lịch.
     * - Đánh dấu isAssignedByAdmin = true.
     */
    TrainerBookingResponse assignTrainerByAdmin(Long adminId, Long bookingId, Long trainerId);

    /**
     * Admin hủy bất kỳ booking nào.
     */
    TrainerBookingResponse cancelBookingByAdmin(Long adminId, Long bookingId, String reason);

    /**
     * Lấy tất cả booking chưa được gán trainer (admin dashboard).
     * categoryId null → lấy tất cả.
     */
    List<TrainerBookingResponse> getUnassignedBookings(Long categoryId);

    /**
     * Lấy tất cả booking bị từ chối (admin xem để xử lý).
     */
    List<TrainerBookingResponse> getAllRejectedBookings();
}
