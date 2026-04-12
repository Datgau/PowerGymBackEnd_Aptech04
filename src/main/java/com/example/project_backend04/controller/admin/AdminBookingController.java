package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.TrainerWorkingHours.SaveWorkingHoursRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.dto.response.TrainerWorkingHours.TrainerScheduleResponse;
import com.example.project_backend04.service.IService.ITrainerBookingService;
import com.example.project_backend04.service.IService.ITrainerWorkingHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin quản lý booking:
 * - Xem booking chưa được gán trainer (unassigned)
 * - Gán trainer cho booking
 * - Xem booking bị từ chối (để xử lý hoặc reopen)
 * - Hủy bất kỳ booking nào
 * - Quản lý working hours thay cho trainer (nếu cần)
 *
 * Base path: /api/admin/bookings
 */
@RestController
@RequestMapping("api/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminBookingController {

    private final ITrainerBookingService bookingService;
    private final ITrainerWorkingHoursService workingHoursService;

    // ── Dashboard: Booking overview ─────────────────────────────────────────────

    /**
     * GET /api/admin/bookings/unassigned
     * Lấy tất cả booking chưa được gán trainer (user không chọn trainer khi đặt).
     * ?categoryId=1 → lọc theo category (optional)
     */
    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getUnassignedBookings(
            @RequestParam(required = false) Long categoryId) {
        try {
            return ok(bookingService.getUnassignedBookings(categoryId),
                    "Lấy danh sách booking chưa gán trainer thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * GET /api/admin/bookings/rejected
     * Lấy tất cả booking bị trainer từ chối (để admin xử lý / reopen).
     */
    @GetMapping("/rejected")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getRejectedBookings() {
        try {
            return ok(bookingService.getAllRejectedBookings(),
                    "Lấy danh sách booking bị từ chối thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Admin gán trainer ───────────────────────────────────────────────────────

    /**
     * POST /api/admin/bookings/{bookingId}/assign-trainer
     * Admin gán trainer cho booking chưa có trainer.
     * ?trainerId=5
     *
     * Validate:
     * - booking.trainer IS NULL
     * - trainer có specialty phù hợp với gói dịch vụ
     * - trainer không trùng lịch
     */
    @PostMapping("/{bookingId}/assign-trainer")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> assignTrainer(
            @PathVariable Long bookingId,
            @RequestParam Long adminId,
            @RequestParam Long trainerId) {
        try {
            return ok(bookingService.assignTrainerByAdmin(adminId, bookingId, trainerId),
                    "Gán trainer thành công");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return serverError(e.getMessage());
        }
    }

    // ── Admin hủy booking ───────────────────────────────────────────────────────

    /**
     * PUT /api/admin/bookings/{bookingId}/cancel
     * Admin hủy bất kỳ booking nào (trừ COMPLETED, CANCELLED).
     */
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {
        try {
            return ok(bookingService.cancelBookingByAdmin(adminId, bookingId, reason),
                    "Hủy booking thành công");
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }
    /**
     * GET /api/admin/bookings/trainers/{trainerId}/schedule
     * Xem lịch làm việc của trainer (để admin hỗ trợ cấu hình).
     */
    @GetMapping("/trainers/{trainerId}/schedule")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> getTrainerWeeklySchedule(
            @PathVariable Long trainerId) {
        try {
            return ok(workingHoursService.getWeeklySchedule(trainerId),
                    "Lấy lịch làm việc trainer thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * GET /api/admin/bookings/trainers/{trainerId}/daily-availability?date=2025-04-01
     * Xem lịch AVAILABLE/BOOKED/DAY_OFF của trainer theo ngày cụ thể.
     */
    @GetMapping("/trainers/{trainerId}/daily-availability")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> getTrainerDailyAvailability(
            @PathVariable Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ok(workingHoursService.getDailyAvailability(trainerId, date),
                    "Lấy lịch trống trainer thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * PUT /api/admin/bookings/trainers/{trainerId}/schedule
     * Admin thiết lập lịch làm việc cho trainer (dùng khi trainer mới, chưa tự setup).
     */
    @PutMapping("/trainers/{trainerId}/schedule")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> setTrainerSchedule(
            @PathVariable Long trainerId,
            @Valid @RequestBody SaveWorkingHoursRequest request) {
        try {
            return ok(workingHoursService.saveWeeklySchedule(trainerId, request),
                    "Cập nhật lịch làm việc cho trainer thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data, String msg) {
        return ResponseEntity.ok(new ApiResponse<>(true, msg, data, 200));
    }

    private static <T> ResponseEntity<ApiResponse<T>> badRequest(String msg) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, msg, null, 400));
    }

    private static <T> ResponseEntity<ApiResponse<T>> conflict(String msg) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, msg, null, 409));
    }

    private static <T> ResponseEntity<ApiResponse<T>> serverError(String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, msg, null, 500));
    }
}
