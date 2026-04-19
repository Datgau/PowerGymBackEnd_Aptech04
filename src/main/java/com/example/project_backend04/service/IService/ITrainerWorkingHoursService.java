package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.TrainerWorkingHours.SaveWorkingHoursRequest;
import com.example.project_backend04.dto.response.TrainerWorkingHours.TrainerScheduleResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Interface quản lý lịch làm việc (Working Hours) của trainer.
 * Dùng cho:
 * - Trainer/Admin thiết lập lịch làm việc theo tuần
 * - Frontend hiển thị lịch trống khi user đặt booking
 * - Validate booking không nằm ngoài giờ làm việc
 */
public interface ITrainerWorkingHoursService {

    // ── Trainer / Admin quản lý lịch ─────────────────────────────────────────────

    /**
     * Lưu (tạo mới hoặc cập nhật) toàn bộ lịch làm việc trong tuần của trainer.
     * Request chứa danh sách slot theo từng ngày trong tuần.
     * Các slot cũ sẽ được deactivate, slot mới được upsert.
     */
    TrainerScheduleResponse saveWeeklySchedule(Long trainerId, SaveWorkingHoursRequest request);

    /**
     * Lấy toàn bộ lịch tuần của trainer (để trainer/admin xem và chỉnh sửa).
     */
    TrainerScheduleResponse getWeeklySchedule(Long trainerId);

    /**
     * Đánh dấu một ngày là ngày nghỉ (day off).
     */
    void markDayOff(Long trainerId, DayOfWeek dayOfWeek, boolean isDayOff);

    /**
     * Bật/tắt một slot cụ thể.
     */
    void toggleSlot(Long slotId, boolean isActive);

    // ── Frontend booking picker ───────────────────────────────────────────────────

    /**
     * Lấy lịch của trainer cho một ngày cụ thể.
     * Kết hợp Working Hours + Existing Bookings để trả về các slot:
     *   - AVAILABLE: slot trống, user có thể đặt
     *   - BOOKED: slot đã có booking CONFIRMED hoặc PENDING
     *   - DAY_OFF: trainer nghỉ ngày này
     * Frontend dùng để render calendar picker.
     */
    TrainerScheduleResponse getDailyAvailability(Long trainerId, LocalDate date);

    /**
     * Lấy lịch của nhiều trainer cho một ngày (dùng trang "Chọn trainer").
     */
    List<TrainerScheduleResponse> getDailyAvailabilityForTrainers(
        List<Long> trainerIds, LocalDate date);

    // ── Validation ────────────────────────────────────────────────────────────────

    /**
     * Kiểm tra xem khoảng giờ [startTime, endTime] ở ngày [date]
     * có nằm trong giờ làm việc của trainer không.
     * Dùng khi validate booking request.
     */
    boolean isWithinWorkingHours(Long trainerId, LocalDate date,
                                  LocalTime startTime, LocalTime endTime);
    /**
     * Xin nghỉ theo ngày cụ thể (không phải DayOfWeek lặp lại).
     * - allDay = true  → set isDayOff=true cho tất cả slot của ngày đó
     * - allDay = false → set isActive=false cho các slotIds được chọn
     */
    void requestDayOff(Long trainerId, com.example.project_backend04.dto.request.TrainerWorkingHours.DayOffRequest request);
}
