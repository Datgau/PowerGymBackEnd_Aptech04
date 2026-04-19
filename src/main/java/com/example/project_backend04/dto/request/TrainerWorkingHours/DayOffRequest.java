package com.example.project_backend04.dto.request.TrainerWorkingHours;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request xin nghỉ theo ngày cụ thể.
 * - allDay = true  → đánh dấu toàn bộ slot của ngày đó là isDayOff
 * - allDay = false → chỉ tắt các slot có ID trong slotIds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayOffRequest {

    @NotNull(message = "date is required")
    private LocalDate date;

    /** true = nghỉ cả ngày, false = nghỉ theo slot */
    @NotNull(message = "allDay is required")
    private Boolean allDay;

    /**
     * Danh sách slot ID cần tắt (chỉ dùng khi allDay = false).
     * Nếu allDay = true thì bỏ qua field này.
     */
    private List<Long> slotIds;

    /** Lý do xin nghỉ (tuỳ chọn) */
    private String reason;
}
