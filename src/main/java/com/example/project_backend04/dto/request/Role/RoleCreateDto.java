package com.example.project_backend04.dto.request.Role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleCreateDto {

    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;

    private String description;
}
