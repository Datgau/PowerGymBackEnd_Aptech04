package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.TrainerBooking.CreateBookingRequest;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller cho User — đặt lịch, hủy, xem lịch của mình,
 * và xem lịch trống của trainer trước khi đặt.
 *
 * Base path: /api/bookings
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class UserBookingController {

    private final ITrainerBookingService bookingService;
    private final ITrainerWorkingHoursService workingHoursService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> createBooking(
            @PathVariable Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        try {
            TrainerBookingResponse booking = bookingService.createBooking(userId, request);
            return ok(booking, "Tạo lịch hẹn thành công");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return serverError(e.getMessage());
        }
    }

    //  User xem lịch của mình
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getMyBookings(
            @PathVariable Long userId,
            @RequestParam(required = false) String status) {
        try {
            List<TrainerBookingResponse> bookings = bookingService.getMyBookings(userId, status);
            return ok(bookings, "Lấy danh sách booking thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // User hủy booking

    /**
     * PUT /api/bookings/{bookingId}/cancel
     * User hủy booking của mình (chỉ được hủy ≥ 2h trước giờ hẹn).
     */
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {
        try {
            TrainerBookingResponse booking = bookingService.cancelBookingByUser(userId, bookingId, reason);
            return ok(booking, "Hủy lịch hẹn thành công");
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    //Xem lịch trống của trainer (trước khi đặt)

    /**
     * GET /api/bookings/trainers/{trainerId}/schedule?date=2025-04-01
     * Xem lịch trống / bận của trainer theo ngày.
     * Frontend dùng để render calendar picker.
     */
    @GetMapping("/trainers/{trainerId}/schedule")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> getTrainerDailySchedule(
            @PathVariable Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            TrainerScheduleResponse schedule =
                    workingHoursService.getDailyAvailability(trainerId, date);
            return ok(schedule, "Lấy lịch trainer thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * GET /api/bookings/trainers/{trainerId}/weekly-schedule
     * Xem lịch làm việc cả tuần (static, không tính booking).
     */
    @GetMapping("/trainers/{trainerId}/weekly-schedule")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> getTrainerWeeklySchedule(
            @PathVariable Long trainerId) {
        try {
            TrainerScheduleResponse schedule = workingHoursService.getWeeklySchedule(trainerId);
            return ok(schedule, "Lấy lịch tuần trainer thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    //Helpers

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

    private static <T> ResponseEntity<ApiResponse<T>> forbidden(String msg) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, msg, null, 403));
    }

    private static <T> ResponseEntity<ApiResponse<T>> serverError(String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, msg, null, 500));
    }
}
