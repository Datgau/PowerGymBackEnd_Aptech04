package com.example.project_backend04.dto.request.TrainerBooking;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request gửi lên khi trainer từ chối một booking.
 * File/hình ảnh đính kèm (nếu có) phải được upload qua Cloudinary trước.
 * URL sau khi upload sẽ truyền vào field rejectionMediaUrl.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectBookingRequest {

    /**
     * Lý do từ chối - bắt buộc.
     */
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    /**
     * URL file/hình ảnh minh họa lý do từ chối - tùy chọn.
     * Đã được upload lên Cloudinary (folder: booking_rejections).
     */
    private String rejectionMediaUrl;
}
