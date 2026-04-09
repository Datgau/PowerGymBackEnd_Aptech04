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
import java.util.Map;

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

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateBooking(
            @RequestParam Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        try {
            // Check time slot conflicts for user
            boolean hasConflict = bookingService.checkUserTimeSlotConflict(
                userId, 
                request.getBookingDate(), 
                request.getStartTime(), 
                request.getEndTime()
            );
            
            if (hasConflict) {
                return conflict("You already have a booking in this time slot");
            }
            
            // Check trainer conflicts if trainer is specified
            if (request.getTrainerId() != null) {
                boolean trainerHasConflict = bookingService.checkTrainerTimeSlotConflict(
                    request.getTrainerId(),
                    request.getBookingDate(),
                    request.getStartTime(),
                    request.getEndTime()
                );
                
                if (trainerHasConflict) {
                    return conflict("Trainer is already booked for this time slot");
                }
            }
            
            return ok("Time slot is available", "Validation successful");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
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

    /**
     * Get list of dates in a month that have bookings for a trainer
     * Used to disable dates in date picker
     */
    @GetMapping("/trainers/{trainerId}/booked-dates")
    public ResponseEntity<ApiResponse<Map<String, List<LocalDate>>>> getTrainerBookedDates(
            @PathVariable Long trainerId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            Map<String, List<LocalDate>> result = bookingService.getTrainerBookedDatesInMonth(trainerId, year, month);
            return ok(result, "Lấy danh sách ngày đã có lịch thành công");
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
