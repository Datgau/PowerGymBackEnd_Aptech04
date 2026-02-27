package com.example.project_backend04.dto.request.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "OTP không được để trống")
    @Size(min = 4, max = 6, message = "OTP phải có từ 4 đến 6 ký tự")
    @Pattern(regexp = "^[0-9]+$", message = "OTP chỉ được chứa số")
    private String otp;
}
