package com.example.project_backend04.dto.request.Trainer;

import com.example.project_backend04.enums.ServiceCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTrainerRequest {
    
    // Thông tin cơ bản
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;
    
    @Size(max = 1000, message = "Tiểu sử không được quá 1000 ký tự")
    private String bio;
    
    // Thông tin chuyên môn
    @NotNull(message = "Phải có ít nhất một chuyên môn")
    @Size(min = 1, message = "Trainer phải có ít nhất một chuyên môn")
    private List<TrainerSpecialtyRequest> specialties;
    
    // Thông tin bổ sung
    private Integer totalExperienceYears; // Tổng số năm kinh nghiệm
    private String education; // Trình độ học vấn
    private String emergencyContact; // Liên hệ khẩn cấp
    private String emergencyPhone; // SĐT khẩn cấp
    
    @Data
    public static class TrainerSpecialtyRequest {
        @NotNull(message = "Chuyên môn không được để trống")
        private ServiceCategory specialty;
        
        @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
        private String description;
        
        private Integer experienceYears;
        
        @Size(max = 500, message = "Chứng chỉ không được quá 500 ký tự")
        private String certifications;
        
        @Size(max = 20, message = "Trình độ không được quá 20 ký tự")
        private String level; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
}