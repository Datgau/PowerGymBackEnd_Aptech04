package com.example.project_backend04.dto.response.TrainerWorkingHours;

import com.example.project_backend04.enums.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Response trả về lịch làm việc của trainer.
 * Dùng cho:
 * - Trainer/Admin xem và chỉnh sửa lịch tuần
 * - Frontend booking picker hiển thị slot available/booked
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerScheduleResponse {

    private Long trainerId;
    private String trainerName;
    private String trainerAvatar;

    /**
     * Lịch theo ngày trong tuần (dùng cho xem lịch tuần).
     * Key = DayOfWeek (MONDAY, TUESDAY, ...)
     */
    private Map<DayOfWeek, List<SlotInfo>> weeklySchedule;
    private LocalDate date;
    private List<SlotInfo> dailySlots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotInfo {

        private Long slotId;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private SlotStatus status;
        private String note;

        private Boolean isDayOff;

        /** Booking ID nếu slot đã bị book (để frontend hiển thị thông tin) */
        private Long bookingId;


    }
}
