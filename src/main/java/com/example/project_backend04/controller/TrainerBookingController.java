package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.TrainerBooking.RejectBookingRequest;
import com.example.project_backend04.dto.request.TrainerWorkingHours.SaveWorkingHoursRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.dto.response.TrainerWorkingHours.TrainerScheduleResponse;
import com.example.project_backend04.service.IService.ICloudinaryService;
import com.example.project_backend04.service.IService.ITrainerBookingService;
import com.example.project_backend04.service.IService.ITrainerWorkingHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Controller cho Trainer:
 * - Xem / chấp nhận / từ chối booking (có thể đính kèm file ảnh khi từ chối)
 * - Quản lý lịch làm việc (working hours)
 *
 * Base path: /api/trainer
 */
@RestController
@RequestMapping("/api/trainer")
@RequiredArgsConstructor
public class TrainerBookingController {

    private final ITrainerBookingService bookingService;
    private final ITrainerWorkingHoursService workingHoursService;
    private final ICloudinaryService cloudinaryService;


    @GetMapping("/{trainerId}/bookings/pending")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getPendingBookings(
            @PathVariable Long trainerId) {
        try {
            return ok(bookingService.getPendingBookingsForTrainer(trainerId),
                    "Lấy danh sách booking đang chờ thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/{trainerId}/bookings/upcoming")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getUpcomingBookings(
            @PathVariable Long trainerId) {
        try {
            return ok(bookingService.getUpcomingBookingsForTrainer(trainerId),
                    "Lấy danh sách booking sắp tới thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }


    @PutMapping("/{trainerId}/bookings/{bookingId}/accept")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> acceptBooking(
            @PathVariable Long trainerId,
            @PathVariable Long bookingId,
            @RequestParam(required = false) String notes) {
        try {
            return ok(bookingService.acceptBooking(trainerId, bookingId, notes),
                    "Xác nhận lịch hẹn thành công");
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping(value = "/{trainerId}/bookings/{bookingId}/reject",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> rejectBookingJson(
            @PathVariable Long trainerId,
            @PathVariable Long bookingId,
            @Valid @RequestBody RejectBookingRequest request) {
        try {
            return ok(bookingService.rejectBooking(trainerId, bookingId, request),
                    "Từ chối lịch hẹn thành công");
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping(value = "/{trainerId}/bookings/{bookingId}/reject",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> rejectBookingWithFile(
            @PathVariable Long trainerId,
            @PathVariable Long bookingId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            String mediaUrl = null;
            if (file != null && !file.isEmpty()) {
                mediaUrl = cloudinaryService.uploadSingleFile(file, "booking_rejections");
            }

            RejectBookingRequest req = RejectBookingRequest.builder()
                    .rejectionReason(reason)
                    .rejectionMediaUrl(mediaUrl)
                    .build();

            return ok(bookingService.rejectBooking(trainerId, bookingId, req),
                    "Từ chối lịch hẹn thành công");
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/{trainerId}/schedule")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> getWeeklySchedule(
            @PathVariable Long trainerId) {
        try {
            return ok(workingHoursService.getWeeklySchedule(trainerId),
                    "Lấy lịch làm việc thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }


    @PutMapping("/{trainerId}/schedule")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> saveWeeklySchedule(
            @PathVariable Long trainerId,
            @Valid @RequestBody SaveWorkingHoursRequest request) {
        try {
            return ok(workingHoursService.saveWeeklySchedule(trainerId, request),
                    "Cập nhật lịch làm việc thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }


    @PatchMapping("/{trainerId}/schedule/day-off")
    public ResponseEntity<ApiResponse<Void>> markDayOff(
            @PathVariable Long trainerId,
            @RequestParam DayOfWeek day,
            @RequestParam boolean isDayOff) {
        try {
            workingHoursService.markDayOff(trainerId, day, isDayOff);
            String msg = isDayOff
                    ? day + " đã được đánh dấu là ngày nghỉ"
                    : day + " đã bỏ đánh dấu ngày nghỉ";
            return ok(null, msg);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }


    @PatchMapping("/schedule/slots/{slotId}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleSlot(
            @PathVariable Long slotId,
            @RequestParam boolean isActive) {
        try {
            workingHoursService.toggleSlot(slotId, isActive);
            return ok(null, "Cập nhật slot thành công");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────

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
}