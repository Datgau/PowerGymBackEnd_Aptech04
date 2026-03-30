package com.example.project_backend04.dto.request.TrainerWorkingHours;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Request lưu toàn bộ lịch làm việc theo tuần của trainer.
 * Mỗi entry trong slots tương ứng với một slot thời gian trong ngày.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveWorkingHoursRequest {

    /**
     * Danh sách các slot lịch làm việc trong tuần.
     * Mỗi slot định nghĩa một khoảng giờ trong một ngày cụ thể.
     */
    @NotNull(message = "Slots list is required")
    @Valid
    private List<WorkingSlotRequest> slots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkingSlotRequest {

        /** Ngày trong tuần */
        @NotNull(message = "Day of week is required")
        private DayOfWeek dayOfWeek;

        /** Giờ bắt đầu slot */
        @NotNull(message = "Start time is required")
        private LocalTime startTime;

        /** Giờ kết thúc slot */
        @NotNull(message = "End time is required")
        private LocalTime endTime;

        /** Có phải ngày nghỉ không (true = cả ngày nghỉ, bỏ qua startTime/endTime) */
        private Boolean isDayOff = false;

        /** Ghi chú về slot (VD: "Chỉ nhận PT 1-1", "Chỉ nhận beginner") */
        private String note;
    }
}
